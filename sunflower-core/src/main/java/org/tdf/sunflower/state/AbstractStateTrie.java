package org.tdf.sunflower.state;

import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.tdf.common.serialize.Codec;
import org.tdf.common.store.CachedStore;
import org.tdf.common.store.DatabaseStore;
import org.tdf.common.store.NoDeleteBatchStore;
import org.tdf.common.store.Store;
import org.tdf.common.trie.ReadOnlyTrie;
import org.tdf.common.trie.SecureTrie;
import org.tdf.common.trie.Trie;
import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.Start;
import org.tdf.sunflower.consensus.vrf.util.ByteArrayMap;
import org.tdf.sunflower.facade.DatabaseStoreFactory;
import org.tdf.sunflower.types.Block;
import org.tdf.sunflower.types.CryptoContext;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

@Slf4j(topic = "trie")
public abstract class AbstractStateTrie<ID, S> implements StateTrie<ID, S> {

    private final String name;

    private DatabaseStore db;

    @Getter
    private Store<byte[], byte[]> trieStore;


    @Getter
    private Trie<ID, S> trie;
    @Getter
    private StateUpdater<ID, S> updater;
    @Getter
    private HexBytes genesisRoot;

    @SneakyThrows
    public AbstractStateTrie(
            StateUpdater<ID, S> updater,
            Codec<ID, byte[]> idCodec,
            Codec<S, byte[]> stateCodec,
            // TODO: verify genesis state roots
            DatabaseStoreFactory factory,
            boolean secure
    ) {
        name = getPrefix() + "-trie";
        this.updater = updater;
        this.db = factory.create(name);
        trieStore = new NoDeleteBatchStore<>(db);

        trie = Trie.<ID, S>builder()
                .hashFunction(CryptoContext::hash)
                .store(trieStore)
                .keyCodec(idCodec)
                .valueCodec(stateCodec).build();

        if (secure)
            trie = new SecureTrie<>(trie, CryptoContext::hash);

        // sync to genesis
        Trie<ID, S> tmp = trie.revert();
        updater.getGenesisStates().forEach(tmp::put);
        genesisRoot = HexBytes.fromBytes(tmp.commit());
        log.info("genesis states = {}", Start.MAPPER.writeValueAsString(updater.getGenesisStates()));
        log.info("genesis state root = " + genesisRoot);
        tmp.flush();
    }

    protected abstract String getPrefix();

    public Optional<S> get(byte[] rootHash, ID id) {
        return getTrieForReadOnly(rootHash).get(id);
    }

    @Override
    public Map<byte[], byte[]> getProof(byte[] rootHash, Collection<? extends ID> ids) {
        return getTrieForReadOnly(rootHash)
                .getProof(ids);
    }

    protected Trie<ID, S> commitInternal(byte[] parentRoot, Map<ID, S> data) {
        Store<byte[], byte[]> cache = new CachedStore<>(getTrieStore(), ByteArrayMap::new);
        Trie<ID, S> trie = getTrie().revert(parentRoot, cache);
        data.forEach(trie::put);
        trie.commit();
        return trie;
    }

    public Trie<ID, S> getTrie(byte[] rootHash) {
        return getTrie().revert(rootHash);
    }

    @SneakyThrows
    protected Trie<ID, S> getTrieForReadOnly(byte[] rootHash) {
        return ReadOnlyTrie.of(getTrie(rootHash));
    }


    @Override
    public Trie<ID, S> tryUpdate(byte[] parentRoot, Block block) {
        if (!getTrieStore().containsKey(parentRoot)) {
            log.error("update failed: trie root {} at height {} hash {} not found", HexBytes.fromBytes(parentRoot), block.getHeight() - 1, block.getHashPrev());
        }

        Store<byte[], byte[]> cache = new CachedStore<>(getTrieStore(), org.tdf.common.util.ByteArrayMap::new);


        // get a trie at parent block's state
        // modifications to the trie will not persisted until flush() called
        Trie<ID, S> tmp =
                getTrie()
                        .revert(parentRoot, cache);

        getUpdater().update(tmp.asMap(), block);
        return tmp;
    }

    @Override
    public Trie<ID, S> commit(byte[] parent, Map<ID, S> states) {
        return commitInternal(parent, states);
    }

    @Override
    public void prune(Collection<? extends byte[]> excludedRoots) {
        getTrie().prune(excludedRoots, db);
    }


}
