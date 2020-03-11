package org.tdf.sunflower.state;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.Getter;
import lombok.SneakyThrows;
import org.tdf.common.serialize.Codec;
import org.tdf.common.store.*;
import org.tdf.common.trie.ReadOnlyTrie;
import org.tdf.common.trie.SecureTrie;
import org.tdf.common.trie.Trie;
import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.consensus.vrf.util.ByteArraySet;
import org.tdf.sunflower.crypto.CryptoContext;
import org.tdf.sunflower.ApplicationConstants;
import org.tdf.sunflower.consensus.vrf.util.ByteArrayMap;
import org.tdf.sunflower.db.DatabaseStoreFactory;
import org.tdf.sunflower.types.Block;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Collectors;

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
    private Cache<HexBytes, Trie<ID, S>> cache =
            CacheBuilder.newBuilder()
                    .maximumSize(ApplicationConstants.TRIE_CACHE_SIZE)
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
        this.db = factory.create(name);
        trieStore = new NoDeleteBatchStore<>(db);

        trie = new SecureTrie<>(Trie.<ID, S>builder()
                .hashFunction(CryptoContext::digest)
                .store(trieStore)
                .keyCodec(idCodec)
                .valueCodec(stateCodec).build(),
                CryptoContext.hashFunction
        )
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


    public Trie<ID, S> update(byte[] parentRoot, Block block) {
        Trie<ID, S> trie = getTrie().revert(parentRoot);
        Set<ID> relatedIds = getUpdater().getRelatedKeys(
                block,
                trie.asMap()
        );
        Map<ID, S> map = updater.createEmptyMap();
        relatedIds.forEach(k -> map.put(k, trie.get(k).orElse(updater.createEmpty(k))));

        return commitInternal(
                parentRoot,
                getUpdater().update(
                        map,
                        block
                )
        );
    }

    @Override
    public void prune(Collection<? extends byte[]> excludedRoots) {
        getTrie().prune(excludedRoots, db);
    }
}
