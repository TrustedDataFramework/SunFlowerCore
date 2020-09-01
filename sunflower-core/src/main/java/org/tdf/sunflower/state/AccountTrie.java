package org.tdf.sunflower.state;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.tdf.common.serialize.Codecs;
import org.tdf.common.store.ReadOnlyStore;
import org.tdf.common.store.Store;
import org.tdf.common.trie.Trie;
import org.tdf.common.util.HexBytes;
import org.tdf.lotusvm.ModuleInstance;
import org.tdf.sunflower.facade.BasicMessageQueue;
import org.tdf.sunflower.facade.DatabaseStoreFactory;
import org.tdf.sunflower.vm.abi.Context;
import org.tdf.sunflower.vm.hosts.ContractDB;
import org.tdf.sunflower.vm.hosts.Hosts;
import org.tdf.sunflower.vm.hosts.Limit;
import org.tdf.sunflower.vm.hosts.UnsupportedTransfer;

import java.util.Collections;
import java.util.Map;

@Getter
@Slf4j(topic = "trie")
public class AccountTrie extends AbstractStateTrie<HexBytes, Account> {
    private Trie<byte[], byte[]> contractStorageTrie;
    private Store<byte[], byte[]> contractCodeStore;
    private BasicMessageQueue messageQueue;

    public AccountTrie(
            StateUpdater<HexBytes, Account> updater,
            DatabaseStoreFactory factory,
            Store<byte[], byte[]> contractCodeStore,
            Trie<byte[], byte[]> contractStorageTrie,
            BasicMessageQueue messageQueue,
            boolean secure
    ) {
        super(updater, Codecs.newRLPCodec(HexBytes.class), Codecs.newRLPCodec(Account.class), factory, secure);
        this.contractStorageTrie = contractStorageTrie;
        this.contractCodeStore = contractCodeStore;
        this.messageQueue = messageQueue;
    }

    public byte[] view(byte[] stateRoot, HexBytes address, HexBytes args) {
        Account account = getTrie(stateRoot)
                .get(address)
                .filter(Account::containsContract)
                .orElseThrow(() -> new RuntimeException(address + " not exists or not a contract address"));

        Context ctx =
                new Context(null, null, account, args);

        Trie<byte[], byte[]> trie =
                contractStorageTrie.revert(account.getStorageRoot(), ReadOnlyStore.of(contractStorageTrie.getStore()));

        Hosts hosts = new Hosts()
                .withTransfer(new UnsupportedTransfer())
                .withContext(ctx)
                .withDB(new ContractDB(trie))
                .withEvent(messageQueue, address);

        ModuleInstance instance = ModuleInstance.builder()
                .hooks(Collections.singleton(new Limit()))
                .binary(contractCodeStore.get(account.getContractHash()).get())
                .hostFunctions(hosts.getAll())
                .build();


        instance.execute(ctx.getMethod());
        return hosts.getResult();
    }

    @Override
    protected Trie<HexBytes, Account> commitInternal(byte[] parentRoot, Map<HexBytes, Account> data) {
        data.remove(Constants.FEE_ACCOUNT_ADDR);
        return super.commitInternal(parentRoot, data);
    }

    @Override
    protected String getPrefix() {
        return "account";
    }


}
