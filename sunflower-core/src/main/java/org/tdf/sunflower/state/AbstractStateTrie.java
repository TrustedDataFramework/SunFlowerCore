package org.tdf.sunflower.state;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.tdf.common.store.CachedStore;
import org.tdf.common.store.DatabaseStore;
import org.tdf.common.store.Store;
import org.tdf.common.trie.ReadOnlyTrie;
import org.tdf.common.trie.Trie;
import org.tdf.common.util.ByteArrayMap;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

@Slf4j(topic = "trie")
public abstract class AbstractStateTrie<ID, S> implements StateTrie<ID, S> {
    abstract DatabaseStore getDB();

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
    public void prune(Collection<? extends byte[]> excludedRoots) {
        getTrie().prune(excludedRoots, getDB());
    }

}
