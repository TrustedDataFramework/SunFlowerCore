package org.tdf.sunflower.state;

import org.tdf.common.store.Store;
import org.tdf.common.trie.Trie;
import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.types.Block;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * state storage
 *
 * @param <ID> identifier of state
 * @param <S>  state
 */
public interface StateTrie<ID, S> {
    // get an optional state at a root hash
    Optional<S> get(byte[] rootHash, ID id);

    // get values batched
    Map<ID, S> batchGet(byte[] rootHash, Collection<? extends ID> ids);

    HexBytes getGenesisRoot();

    Map<byte[], byte[]> getProof(byte[] rootHash, Collection<? extends ID> ids);

    Trie<ID, S> getTrie();

    Trie<ID, S> getTrie(byte[] rootHash);

    StateUpdater<ID, S> getUpdater();

    Store<byte[], byte[]> getTrieStore();

    // get new trie without make any modification to underlying database
    // call trie.flush() to persist modifications
    Trie<ID, S> update(byte[] parentRoot, Block block);

    // collect garbage
    void gc(Collection<? extends byte[]> excludedRoots);
}
