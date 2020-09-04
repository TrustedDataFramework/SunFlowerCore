package org.tdf.sunflower.state;

import org.tdf.common.store.Store;
import org.tdf.common.trie.Trie;
import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.types.Block;
import org.tdf.sunflower.types.Header;
import org.tdf.sunflower.types.Transaction;

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

    HexBytes getGenesisRoot();

    Map<byte[], byte[]> getProof(byte[] rootHash, Collection<? extends ID> ids);

    Trie<ID, S> getTrie();

    Trie<ID, S> getTrie(byte[] rootHash);

    Store<byte[], byte[]> getTrieStore();

    // collect garbage
    void prune(Collection<? extends byte[]> excludedRoots);

    ForkedStateTrie<ID, S> fork(byte[] parentRoot);
}
