package org.tdf.sunflower.state;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.ListUtils;
import org.tdf.common.serialize.Codecs;
import org.tdf.common.store.DatabaseStore;
import org.tdf.common.store.NoDeleteBatchStore;
import org.tdf.common.store.Store;
import org.tdf.common.trie.SecureTrie;
import org.tdf.common.trie.Trie;
import org.tdf.common.types.Parameters;
import org.tdf.common.types.Uint256;
import org.tdf.common.util.HexBytes;
import org.tdf.rlp.RLPCodec;
import org.tdf.sunflower.Start;
import org.tdf.sunflower.pool.BackendImpl;
import org.tdf.sunflower.types.CryptoContext;
import org.tdf.sunflower.types.Header;
import org.tdf.sunflower.vm.Backend;
import org.tdf.sunflower.vm.CallData;
import org.tdf.sunflower.vm.VMExecutor;
import org.tdf.sunflower.vm.abi.ContractCallPayload;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Slf4j(topic = "trie")
@AllArgsConstructor
public class AccountTrie extends AbstractStateTrie<HexBytes, Account> {
    private Trie<HexBytes, Account> trie;
    private final Trie<HexBytes, HexBytes> contractStorageTrie;
    private final Store<HexBytes, HexBytes> contractCodeStore;
    private Map<HexBytes, BuiltinContract> bios;
    private Map<HexBytes, BuiltinContract> builtins;
    private final DatabaseStore db;
    private final Store<byte[], byte[]> trieStore;

    public Backend createBackend(
        Header parent,
        HexBytes root,
        Long newBlockCreatedAt,
        boolean isStatic
    ) {
        return new BackendImpl(
            parent,
            null,
            trie.revert(root),
            contractStorageTrie,
            new HashMap<>(),
            new HashMap<>(),
            builtins,
            bios,
            isStatic,
            contractCodeStore,
            new HashMap<>(),
            newBlockCreatedAt
        );
    }

    @SneakyThrows
    public AccountTrie(
        DatabaseStore db,
        Store<HexBytes, HexBytes> contractCodeStore,
        Trie<HexBytes, HexBytes> contractStorageTrie,
        boolean secure
    ) {
        this.db = db;
        this.trieStore = new NoDeleteBatchStore<>(db, Store.IS_NULL);

        this.trie = Trie.<HexBytes, Account>builder()
            .hashFunction(CryptoContext::hash)
            .store(trieStore)
            .keyCodec(Codecs.newRLPCodec(HexBytes.class))
            .valueCodec(Codecs.newRLPCodec(Account.class))
            .build();

        if (secure)
            trie = new SecureTrie<>(trie, CryptoContext::hash);

        this.contractStorageTrie = contractStorageTrie;
        this.contractCodeStore = contractCodeStore;
    }

    @Override
    @SneakyThrows
    public HexBytes init(List<Account> alloc, List<BuiltinContract> bios, List<BuiltinContract> builtins) {
        this.builtins = new HashMap<>();
        for (BuiltinContract builtin : builtins) {
            this.builtins.put(builtin.getAddress(), builtin);
        }

        this.bios = new HashMap<>();
        for (BuiltinContract b : bios) {
            this.bios.put(b.getAddress(), b);
        }

        Map<HexBytes, Account> genesisStates = new HashMap<>();

        for (Account account : alloc) {
            genesisStates.put(account.getAddress(), account);
        }


        for (BuiltinContract c : ListUtils.sum(bios, builtins)) {
            HexBytes address = c.getAddress();
            Account a = Account.emptyAccount(address, Uint256.ZERO);
            Trie<HexBytes, HexBytes> trie = contractStorageTrie.revert();
            for (Map.Entry<HexBytes, HexBytes> entry : c.getGenesisStorage().entrySet()) {
                trie.put(entry.getKey(), entry.getValue());
            }
            HexBytes root = trie.commit();
            trie.flush();
            a.setStorageRoot(root);
            genesisStates.put(address, a);
        }

        // sync to genesis
        Trie<HexBytes, Account> tmp = this.trie.revert();
        genesisStates.forEach(tmp::put);
        log.info("genesis states = {}", Start.MAPPER.writeValueAsString(genesisStates));
        HexBytes r = tmp.commit();
        log.info("genesis state root = " + r);
        tmp.flush();
        return r;
    }

    @Override
    DatabaseStore getDB() {
        return this.db;
    }


    public byte[] call(Header header, HexBytes address, String method, Parameters parameters) {
        // execute method
        CallData callData = CallData.empty();
        callData.setTo(address);
        callData.setData(HexBytes.fromBytes(RLPCodec.encode(new ContractCallPayload(method, parameters))));

        Backend backend = createBackend(header, System.currentTimeMillis() / 1000, true);

        VMExecutor executor = new VMExecutor(
            backend,
            callData
        );

        return executor.execute().getExecutionResult();
    }
}
