package org.tdf.sunflower.state;

import lombok.AccessLevel;
import lombok.Getter;
import org.tdf.common.serialize.Codec;
import org.tdf.common.store.CachedStore;
import org.tdf.common.store.NoDeleteStore;
import org.tdf.common.store.Store;
import org.tdf.common.trie.Trie;
import org.tdf.common.util.HexBytes;
import org.tdf.crypto.HashFunctions;
import org.tdf.sunflower.consensus.vrf.util.ByteArrayMap;
import org.tdf.sunflower.db.DatabaseStoreFactory;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

public abstract class AbstractStateTrie<ID, S> implements StateTrie<ID, S> {
    private String TRIE;
    private String DELETED;

    @Getter(AccessLevel.PROTECTED)
    private NoDeleteStore<byte[], byte[]> trieStore;


    @Getter
    private Trie<ID, S> trie;

    protected abstract String getPrefix();

    @Getter(AccessLevel.PROTECTED)
    private StateUpdater<ID, S> updater;

    @Getter
    private byte[] genesisRoot;

    public AbstractStateTrie(
            StateUpdater<ID, S> updater,
            Codec<ID, byte[]> idCodec,
            Codec<S, byte[]> stateCodec,
            // TODO: verify genesis state roots
            DatabaseStoreFactory factory,
            boolean logDeletes
    ) {
        TRIE = getPrefix() + "-trie";
        DELETED = getPrefix() + "-deleted";
        this.updater = updater;

        if (logDeletes) {
            trieStore = new NoDeleteStore<>(
                    factory.create(TRIE),
                    factory.create(DELETED)
            );
        } else {
            trieStore = new NoDeleteStore<>(
                    factory.create(TRIE),
                    Store.getNop()
            );
        }

        trie = Trie.<ID, S>builder()
                .hashFunction(HashFunctions::keccak256)
                .store(trieStore)
                .keyCodec(idCodec)
                .valueCodec(stateCodec).build()
        ;

        // sync to genesis
        Trie<ID, S> tmp = trie.revert();
        updater.getGenesisStates().forEach(tmp::put);
        genesisRoot = tmp.commit();
        tmp.flush();
    }

    public Optional<S> get(byte[] rootHash, ID id) {
        return getTrie().revert(rootHash).get(id);
    }

    public Map<ID, S> batchGet(byte[] rootHash, Collection<ID> keys) {
        Trie<ID, S> trie = getTrie().revert(rootHash);
        Map<ID, S> map = updater.createEmptyMap();
        keys.forEach(
                k -> map.put(k, trie.get(k).orElse(updater.createEmpty(k)))
        );
        return map;
    }

    protected Trie<ID, S> commitInternal(byte[] parentRoot, Map<ID, S> data) {
        Store<byte[], byte[]> cache = new CachedStore<>(getTrieStore(), ByteArrayMap::new);
        Trie<ID, S> trie = getTrie().revert(parentRoot, cache);
        data.forEach(trie::put);
        byte[] newRoot = trie.commit();
        trie.flush();
        return trie;
    }

    public Trie<ID, S> getTrie(byte[] rootHash) {
        return getTrie().revert(rootHash);
    }
}
