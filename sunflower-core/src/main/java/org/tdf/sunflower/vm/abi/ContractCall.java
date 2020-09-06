package org.tdf.sunflower.vm.abi;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.tdf.common.store.Store;
import org.tdf.common.trie.Trie;
import org.tdf.common.types.Uint256;
import org.tdf.common.util.HexBytes;
import org.tdf.lotusvm.ModuleInstance;
import org.tdf.lotusvm.types.Module;
import org.tdf.sunflower.ApplicationConstants;
import org.tdf.sunflower.facade.BasicMessageQueue;
import org.tdf.sunflower.state.Account;
import org.tdf.sunflower.state.SafeMath;
import org.tdf.sunflower.types.CryptoContext;
import org.tdf.sunflower.types.Header;
import org.tdf.sunflower.types.Transaction;
import org.tdf.sunflower.vm.hosts.*;

import java.util.Collections;
import java.util.Map;
import java.util.function.Function;

@RequiredArgsConstructor
@Getter
public class ContractCall {
    // contract address already called
    private final Map<HexBytes, Account> states;
    private final Header header;
    private final Transaction transaction;

    private final Function<byte[], Trie<byte[], byte[]>> storageTrieSupplier;

    // contract code store
    private final Store<byte[], byte[]> contractStore;

    // gas limit hook
    private final Limit limit;

    // call depth
    private final int depth;

    // msg.sender
    private final HexBytes sender;

    private final boolean readonly;

    // contract address called currently
    private HexBytes recipient;

    public static void assertContractAddress(HexBytes address) {
        if (address.size() != Account.ADDRESS_SIZE)
            throw new RuntimeException("invalid address size " + address.size());

        // address starts with 18 zero is reversed
        for (int i = 0; i < 18; i++) {
            if (address.get(i) != 0)
                return;
        }
        throw new RuntimeException("cannot call reversed address " + address);
    }

    public ContractCall fork() {
        if (depth + 1 == ApplicationConstants.MAX_CONTRACT_CALL_DEPTH)
            throw new RuntimeException("exceed call max depth");
        return new ContractCall(
                states,
                header,
                transaction,
                storageTrieSupplier,
                contractStore,
                this.limit.fork(),
                this.depth + 1,
                this.recipient,
                this.readonly
        );
    }


    public byte[] call(HexBytes binaryOrAddress, String method, byte[] parameters, Uint256 amount) {
        boolean isDeploy = "init".equals(method);
        Account contractAccount;
        Account originAccount = readonly ? null : states.get(this.transaction.getFromAddress());
        Module m = null;
        HexBytes contractAddress;

        if (isDeploy) {
            if(this.readonly)
                throw new RuntimeException("cannot deploy contract here");
            m = new Module(binaryOrAddress.getBytes());
            byte[] hash = CryptoContext.hash(binaryOrAddress.getBytes());
            originAccount.setNonce(SafeMath.add(originAccount.getNonce(), 1));
            contractStore.put(hash, binaryOrAddress.getBytes());
            contractAddress = Transaction.createContractAddress(transaction.getFromAddress(), originAccount.getNonce());

            contractAccount = Account.emptyContract(contractAddress);
            contractAccount.setContractHash(hash);
            contractAccount.setCreatedBy(this.transaction.getFromAddress());
            contractAccount.setNonce(originAccount.getNonce());
        } else {
            contractAddress = binaryOrAddress;
            contractAccount = states.get(contractAddress);
        }

        // transfer amount from origin account to contract account
        if(!readonly){
            originAccount.setBalance(originAccount.getBalance().safeSub(amount));
            contractAccount.setBalance(contractAccount.getBalance().safeAdd(amount));
            states.put(contractAccount.getAddress(), contractAccount);
            states.put(originAccount.getAddress(), originAccount);
        }


        this.recipient = contractAddress;
        assertContractAddress(contractAddress);


        // build Parameters here
        Context ctx = new Context(
                header,
                transaction,
                contractAccount,
                Context.buildArguments(method, parameters),
                sender,
                amount
        );

        DBFunctions DBFunctions = new DBFunctions(
                storageTrieSupplier.apply(contractAccount.getStorageRoot()),
                this.readonly
        );

        Hosts hosts = new Hosts()
                .withTransfer(
                        states,
                        this.recipient
                )
                .withRelect(new Reflect(this, readonly))
                .withContext(new ContextHost(ctx, states, contractStore, readonly))
                .withDB(DBFunctions)
                .withEvent(contractAccount.getAddress(), readonly);

        // every contract should have a init method
        ModuleInstance instance = ModuleInstance
                .builder()
                .hooks(Collections.singleton(limit))
                .hostFunctions(hosts.getAll())
                .binary(contractStore.get(contractAccount.getContractHash())
                        .orElseThrow(() -> new RuntimeException(
                                "contract " + this.recipient + " not found in db")))
                .build();


        if (!isDeploy || instance.containsExport("init"))
            instance.execute(method);

        if(!readonly){
            DBFunctions.getStorageTrie().commit();
            contractAccount = states.get(this.recipient);
            contractAccount.setStorageRoot(DBFunctions.getStorageTrie().getRootHash());
            states.put(contractAccount.getAddress(), contractAccount);
        }

        return isDeploy ? contractAddress.getBytes() : hosts.getResult();
    }
}
