package org.tdf.sunflower.state;

import lombok.AccessLevel;
import lombok.Getter;
import org.tdf.common.serialize.Codec;
import org.tdf.common.store.MemoryCachedStore;
import org.tdf.common.store.NoDeleteStore;
import org.tdf.common.store.Store;
import org.tdf.common.trie.Trie;
import org.tdf.common.trie.TrieImpl;
import org.tdf.crypto.HashFunctions;
import org.tdf.sunflower.db.DatabaseStoreFactory;
import org.tdf.sunflower.types.Block;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

public abstract class AbstractStateTrie<ID, T> implements StateTrie<ID, T> {
    private String TRIE;
    private String DELETED;

    @Getter(AccessLevel.PROTECTED)
    private NoDeleteStore<byte[], byte[]> trieStore;


    @Getter
    private Trie<ID, T> trie;

    protected abstract String getPrefix();

    @Getter(AccessLevel.PROTECTED)
    private StateUpdater<ID, T> updater;

    public AbstractStateTrie(
            StateUpdater<ID, T> updater,
            Codec<ID, byte[]> idCodec,
            Codec<T, byte[]> stateCodec,
            // TODO: verify genesis state roots
            Block genesis,
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

        trie = TrieImpl.newInstance(
                HashFunctions::keccak256,
                trieStore,
                idCodec,
                stateCodec
        );

        // sync to genesis
        Trie<ID, T> tmp = trie.revert();
        updater.getGenesisStates().forEach(tmp::put);
        tmp.commit();
        tmp.flush();
    }

    public Optional<T> get(byte[] rootHash, ID id) {
        return getTrie().revert(rootHash).get(id);
    }

    public Map<ID, T> batchGet(byte[] rootHash, Collection<ID> keys) {
        Trie<ID, T> trie = getTrie().revert(rootHash);
        Map<ID, T> map = updater.createEmptyMap();
        keys.forEach(
                k -> map.put(k, trie.get(k).orElse(updater.createEmpty(k)))
        );
        return map;
    }

    protected Trie<ID, T> commitInternal(byte[] parentRoot, Map<ID, T> data) {
        Store<byte[], byte[]> cache = new MemoryCachedStore<>(getTrieStore());
        Trie<ID, T> trie = getTrie().revert(parentRoot, cache);
        data.forEach(trie::put);
        byte[] newRoot = trie.commit();
        trie.flush();
        return trie;
    }

    public Trie<ID, T> getTrie(byte[] rootHash){
        return getTrie().revert(rootHash);
    }
}
