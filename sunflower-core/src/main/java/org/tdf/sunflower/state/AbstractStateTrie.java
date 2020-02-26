package org.tdf.sunflower.state;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.Getter;
import lombok.SneakyThrows;
import org.tdf.common.serialize.Codec;
import org.tdf.common.store.BatchStore;
import org.tdf.common.store.CachedStore;
import org.tdf.common.store.NoDeleteBatchStore;
import org.tdf.common.store.Store;
import org.tdf.common.trie.ReadOnlyTrie;
import org.tdf.common.trie.Trie;
import org.tdf.common.util.HexBytes;
import org.tdf.crypto.CryptoContext;
import org.tdf.sunflower.consensus.vrf.util.ByteArrayMap;
import org.tdf.sunflower.db.DatabaseStoreFactory;
import org.tdf.sunflower.types.Block;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public abstract class AbstractStateTrie<ID, S> implements StateTrie<ID, S> {
    private static final int CACHE_SIZE = 32;

    private final String name;

    @Getter
    private Store<byte[], byte[]> trieStore;


    @Getter
    private Trie<ID, S> trie;
    @Getter
    private StateUpdater<ID, S> updater;
    @Getter
    private HexBytes genesisRoot;
    private Cache<HexBytes, Trie<ID, S>> cache =
            CacheBuilder.newBuilder()
                    .maximumSize(CACHE_SIZE)
                    .build();

    public AbstractStateTrie(
            StateUpdater<ID, S> updater,
            Codec<ID, byte[]> idCodec,
            Codec<S, byte[]> stateCodec,
            // TODO: verify genesis state roots
            DatabaseStoreFactory factory
    ) {
        name = getPrefix() + "-trie";
        this.updater = updater;

        trieStore = new NoDeleteBatchStore<>(
                factory.create(name)
        );


        trie = Trie.<ID, S>builder()
                .hashFunction(CryptoContext::digest)
                .store(trieStore)
                .keyCodec(idCodec)
                .valueCodec(stateCodec).build()
        ;

        // sync to genesis
        Trie<ID, S> tmp = trie.revert();
        updater.getGenesisStates().forEach(tmp::put);
        genesisRoot = HexBytes.fromBytes(tmp.commit());
        tmp.flush();
    }

    protected abstract String getPrefix();

    public Optional<S> get(byte[] rootHash, ID id) {
        return getTrieForReadOnly(rootHash).get(id);
    }

    public Map<ID, S> batchGet(byte[] rootHash, Collection<? extends ID> ids) {
        Trie<ID, S> trie = getTrieForReadOnly(rootHash);
        Map<ID, S> map = updater.createEmptyMap();
        ids.forEach(
                k -> map.put(k, trie.get(k).orElse(updater.createEmpty(k)))
        );
        return map;
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
    private Trie<ID, S> getTrieForReadOnly(byte[] rootHash) {
        return cache.get(HexBytes.fromBytes(rootHash), () -> ReadOnlyTrie.of(getTrie(rootHash)));
    }

    @Override
    public HexBytes commit(byte[] parentRoot, Block block) {
        final Trie<ID, S> modified = commitInternal(
                parentRoot,
                getUpdater().update(batchGetWithEmpty(parentRoot, block), block)
        );

        modified.flush();
        this.cache.asMap().putIfAbsent(HexBytes.fromBytes(modified.getRootHash()), ReadOnlyTrie.of(modified));
        if (!block.getStateRoot().equals(HexBytes.fromBytes(modified.getRootHash())))
            throw new IllegalArgumentException();
        return HexBytes.fromBytes(modified.getRootHash());
    }

    @Override
    public Trie<ID, S> update(byte[] parentRoot, Block block) {
        return commitInternal(
                parentRoot,
                getUpdater().update(
                        batchGetWithEmpty(parentRoot, block),
                        block
                )
        );
    }

    private Map<ID, S> batchGetWithEmpty(byte[] parentRoot, Block block) {
        Set<ID> relatedIds = getUpdater().getRelatedKeys(
                block,
                getTrieForReadOnly(parentRoot).asMap()
        );
        Map<ID, S> map = batchGet(parentRoot, relatedIds);
        relatedIds.forEach(id -> map.putIfAbsent(id, getUpdater().createEmpty(id)));
        return map;
    }

    @Override
    public void gc(Collection<? extends byte[]> excludedRoots) {
        Map<byte[], byte[]> dumped = new ByteArrayMap<>();
        for (byte[] h : excludedRoots) {
            dumped.putAll(getTrieForReadOnly(h).dump());
        }
        getTrieStore().clear();
        if (getTrieStore() instanceof BatchStore) {
            ((BatchStore<byte[], byte[]>) getTrieStore()).putAll(dumped.entrySet());
            return;
        }
        dumped.forEach(getTrieStore()::put);
    }
}
