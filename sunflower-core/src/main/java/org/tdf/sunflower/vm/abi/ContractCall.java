package org.tdf.sunflower.vm.abi;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import org.tdf.common.store.CachedStore;
import org.tdf.common.store.Store;
import org.tdf.common.trie.Trie;
import org.tdf.common.util.ByteArrayMap;
import org.tdf.common.util.HexBytes;
import org.tdf.lotusvm.ModuleInstance;
import org.tdf.sunflower.ApplicationConstants;
import org.tdf.sunflower.facade.BasicMessageQueue;
import org.tdf.sunflower.state.Account;
import org.tdf.sunflower.types.Header;
import org.tdf.sunflower.types.Transaction;
import org.tdf.sunflower.vm.hosts.Call;
import org.tdf.sunflower.vm.hosts.ContractDB;
import org.tdf.sunflower.vm.hosts.Limit;
import org.tdf.sunflower.vm.hosts.Hosts;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ContractCall {
    private Set<HexBytes> called;
    private Map<HexBytes, Account> states;
    private Header header;
    private Transaction transaction;
    private Trie<byte[], byte[]> storageTrie;
    private ContractCall parent;
    private BasicMessageQueue messageQueue;
    private Store<byte[], byte[]> contractStore;
    private Limit limit;
    private int depth;

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

    public ContractCall fork(){
        if(depth > ApplicationConstants.MAX_CONTRACT_CALL_DEPTH)
            throw new RuntimeException("exceed call max depth");
        return new ContractCall(
                called,
                states,
                header,
                transaction,
                storageTrie,
                this,
                messageQueue,
                contractStore,
                this.limit.fork(),
                depth
        );
    }

    public byte[] call(HexBytes address, String method, byte[] parameters) {
        if(called.contains(address))
            throw new RuntimeException("contract call cannot cycled");
        called.add(address);
        assertContractAddress(address);
        Account contractAccount = states.get(address);

        // build Parameters here
        Context ctx = new Context(
                header,
                transaction,
                contractAccount,
                Context.buildArguments(method, parameters)
        );

        ContractDB contractDB = new ContractDB(
                storageTrie.revert(contractAccount.getStorageRoot(),
                        new CachedStore<>(storageTrie.getStore(), ByteArrayMap::new))
        );

        Hosts hosts = new Hosts()
                .withTransfer(
                        states,
                        transaction,
                        contractAccount.getCreatedBy(),
                        contractAccount.getAddress()
                )
                .withCall(new Call(this))
                .withContext(ctx)
                .withDB(contractDB)
                .withEvent(messageQueue, contractAccount.getAddress());

        // every contract should have a init method
        ModuleInstance instance = ModuleInstance
                .builder()
                .hooks(Collections.singleton(limit))
                .hostFunctions(hosts.getAll())
                .binary(contractStore.get(contractAccount.getContractHash())
                        .orElseThrow(() -> new RuntimeException(
                                "contract " + HexBytes.encode(contractAccount.getContractHash()) + " not found in db")))
                .build();

        instance.execute(method);

        contractDB.getStorageTrie().commit();
        contractDB.getStorageTrie().flush();
        contractAccount.setStorageRoot(contractDB.getStorageTrie().getRootHash());

        states.put(contractAccount.getAddress(), contractAccount);
        return hosts.getResult();
    }
}
