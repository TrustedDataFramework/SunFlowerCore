package org.tdf.sunflower.state;

import org.springframework.beans.factory.annotation.Qualifier;
import org.tdf.common.serialize.Codecs;
import org.tdf.common.store.Store;
import org.tdf.common.trie.Trie;
import org.tdf.common.util.HexBytes;
import org.tdf.lotusvm.ModuleInstance;
import org.tdf.sunflower.db.DatabaseStoreFactory;
import org.tdf.sunflower.types.Block;
import org.tdf.sunflower.vm.abi.Context;
import org.tdf.sunflower.vm.hosts.GasLimit;
import org.tdf.sunflower.vm.hosts.Hosts;

import java.util.Collections;

public class AccountTrie extends AbstractStateTrie<HexBytes, Account> {
    private Trie<byte[], byte[]> contractStorageTrie;
    private Store<byte[], byte[]> contractCodeStore;

    public AccountTrie(
            StateUpdater<HexBytes, Account> updater,
            DatabaseStoreFactory factory,
            Store<byte[], byte[]> contractCodeStore,
            Trie<byte[], byte[]> contractStorageTrie
            ) {
        super(updater, Codecs.newRLPCodec(HexBytes.class), Codecs.newRLPCodec(Account.class), factory);
    }

    public byte[] view(byte[] stateRoot, HexBytes address, byte[] parameters) {
        Account account = getTrie(stateRoot)
                .get(address)
                .filter(Account::containsContract)
                .orElseThrow(() -> new RuntimeException(address + " not exists or not a contract address"));

        Context ctx = Context.disabled();
        ctx.setContractAddress(address);
        ctx.setCreatedBy(account.getCreatedBy());

        Hosts hosts = new Hosts()
                .withParameters(parameters, true)
                .withContext(ctx);

        ModuleInstance instance = ModuleInstance.builder()
                .hooks(Collections.singleton(new GasLimit()))
                .binary(contractCodeStore.get(account.getContractHash()).get())
                .hostFunctions(hosts.getAll())
                .build();

        String method = Context.getMethod(parameters);
        instance.execute(method);
        return hosts.getResult();
    }


    @Override
    protected String getPrefix() {
        return "account";
    }
}
