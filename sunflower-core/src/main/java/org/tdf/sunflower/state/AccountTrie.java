package org.tdf.sunflower.state;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.tdf.common.serialize.Codecs;
import org.tdf.common.store.*;
import org.tdf.common.trie.SecureTrie;
import org.tdf.common.trie.Trie;
import org.tdf.common.types.Parameters;
import org.tdf.common.types.Uint256;
import org.tdf.common.util.HexBytes;
import org.tdf.rlp.RLPCodec;
import org.tdf.rlp.RLPList;
import org.tdf.sunflower.Start;
import org.tdf.sunflower.pool.BackendImpl;
import org.tdf.sunflower.types.Block;
import org.tdf.sunflower.types.CryptoContext;
import org.tdf.sunflower.types.Header;
import org.tdf.sunflower.vm.Backend;
import org.tdf.sunflower.vm.CallData;
import org.tdf.sunflower.vm.CallDataImpl;
import org.tdf.sunflower.vm.VMExecutor;
import org.tdf.sunflower.vm.abi.ContractCallPayload;
import org.tdf.sunflower.vm.hosts.Limit;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Getter
@Slf4j(topic = "trie")
@AllArgsConstructor
public class AccountTrie extends AbstractStateTrie<HexBytes, Account> {
    private Trie<HexBytes, Account> trie;
    private final Trie<byte[], byte[]> contractStorageTrie;
    private final Store<byte[], byte[]> contractCodeStore;
    private final List<PreBuiltContract> preBuiltContracts;
    private final Map<HexBytes, Bios> biosList;
    private final Map<HexBytes, Account> genesisStates;
    private final Map<HexBytes, PreBuiltContract> preBuiltContractAddresses;
    private final DatabaseStore db;
    private final Store<byte[], byte[]> trieStore;
    private final HexBytes genesisRoot;

    public Backend createBackend(
            Header parent,
            long newBlockCreatedAt,
            boolean isStatic
    ) {
        return new BackendImpl(
                parent,
                null,
                trie.revert(parent.getStateRoot().getBytes()),
                contractStorageTrie,
                new HashMap<>(),
                new HashMap<>(),
                preBuiltContractAddresses,
                biosList,
                isStatic,
                contractCodeStore,
                new HashMap<>(),
                new HashMap<>()
        );
    }

    @SneakyThrows
    public AccountTrie(
            DatabaseStore db,
            Store<byte[], byte[]> contractCodeStore,
            Trie<byte[], byte[]> contractStorageTrie,
            Map<HexBytes, Account> genesisStates,
            List<PreBuiltContract> preBuiltContracts,
            List<Bios> biosList,
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
        this.preBuiltContracts = preBuiltContracts;
        this.biosList = biosList.stream().collect(
                Collectors.toMap(x -> x.getGenesisAccount().getAddress(), Function.identity())
        );
        this.genesisStates = new HashMap<>(genesisStates);

        List<CommonUpdater> commonUpdaters = new ArrayList<>();
        commonUpdaters.addAll(preBuiltContracts);
        commonUpdaters.addAll(biosList);
        this.preBuiltContractAddresses = new HashMap<>();

        for (CommonUpdater updater : commonUpdaters) {
            Account genesisAccount = updater.getGenesisAccount();
            Trie<byte[], byte[]> trie = contractStorageTrie.revert();
            for (Map.Entry<byte[], byte[]> entry : updater.getGenesisStorage().entrySet()) {
                trie.put(entry.getKey(), entry.getValue());
            }
            byte[] root = trie.commit();
            trie.flush();
            genesisAccount.setStorageRoot(root);
            if (updater instanceof PreBuiltContract) {
                preBuiltContractAddresses.put(updater.getGenesisAccount().getAddress(), (PreBuiltContract) updater);
            }
            this.genesisStates.put(updater.getGenesisAccount().getAddress(), genesisAccount);
        }

        // sync to genesis
        Trie<HexBytes, Account> tmp = this.trie.revert();
        this.genesisStates.forEach(tmp::put);
        this.genesisRoot = HexBytes.fromBytes(tmp.commit());
        log.info("genesis states = {}", Start.MAPPER.writeValueAsString(genesisStates));
        log.info("genesis state root = " + genesisRoot);
        tmp.flush();
    }



    @Override
    DatabaseStore getDB() {
        return this.db;
    }


    public RLPList call(Header header, HexBytes address, String method, Parameters parameters) {
        // execute method
        CallDataImpl callData = CallDataImpl.empty();
        callData.setTo(address);
        callData.setPayload(HexBytes.fromBytes(RLPCodec.encode(new ContractCallPayload(method, parameters))));

        Backend backend = createBackend(header, System.currentTimeMillis() / 1000, true);

        VMExecutor executor = new VMExecutor(
                backend,
                callData,
                new Limit(0, 0, 0, 0),
                0
        );

        return executor.execute().getReturns();
    }
}
