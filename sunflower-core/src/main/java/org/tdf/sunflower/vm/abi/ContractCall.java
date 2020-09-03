package org.tdf.sunflower.vm.abi;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.tdf.common.store.CachedStore;
import org.tdf.common.store.Store;
import org.tdf.common.trie.Trie;
import org.tdf.common.util.ByteArrayMap;
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

@RequiredArgsConstructor
@Getter
public class ContractCall {
    // contract address already called
    private final Map<HexBytes, Account> states;
    private final Header header;
    private final Transaction transaction;

    private final Trie<byte[], byte[]> storageTrie;

    // message queue
    private final BasicMessageQueue messageQueue;

    // contract code store
    private final Store<byte[], byte[]> contractStore;

    // gas limit hook
    private final Limit limit;

    // call depth
    private final int depth;

    // msg.sender
    private final HexBytes sender;

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
                storageTrie,
                messageQueue,
                contractStore,
                this.limit.fork(),
                this.depth + 1,
                this.recipient
        );
    }


    public byte[] call(HexBytes binaryOrAddress, String method, byte[] parameters, long amount) {
        boolean isDeploy = "init".equals(method);
        Account contractAccount;
        Account originAccount = states.get(this.transaction.getFromAddress());
        Module m = null;
        HexBytes contractAddress;

        if (isDeploy) {
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
        originAccount.setBalance(SafeMath.sub(originAccount.getBalance(), amount));
        contractAccount.setBalance(SafeMath.add(contractAccount.getBalance(), amount));

        this.recipient = contractAddress;
        assertContractAddress(contractAddress);

        states.put(contractAccount.getAddress(), contractAccount);
        states.put(originAccount.getAddress(), originAccount);

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
                storageTrie.revert(contractAccount.getStorageRoot(),
                        new CachedStore<>(storageTrie.getStore(), ByteArrayMap::new)),
                false
        );

        Hosts hosts = new Hosts()
                .withTransfer(
                        states,
                        this.recipient
                )
                .withRelect(new Reflect(this))
                .withContext(new ContextHost(ctx, states, contractStore))
                .withDB(DBFunctions)
                .withEvent(messageQueue, contractAccount.getAddress());

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

        DBFunctions.getStorageTrie().commit();
        DBFunctions.getStorageTrie().flush();

        contractAccount = states.get(this.recipient);
        contractAccount.setStorageRoot(DBFunctions.getStorageTrie().getRootHash());
        states.put(contractAccount.getAddress(), contractAccount);
        return isDeploy ? contractAddress.getBytes() : hosts.getResult();
    }
}
