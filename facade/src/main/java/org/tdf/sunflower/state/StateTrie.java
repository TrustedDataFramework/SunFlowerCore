package org.tdf.sunflower.state;

import org.tdf.common.store.Store;
import org.tdf.common.trie.Trie;
import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.types.Header;
import org.tdf.sunflower.vm.Backend;

/**
 * state storage
 *
 * @param <ID> identifier of state
 * @param <S>  state
 */
public interface StateTrie<ID, S> {
    // get an optional state at a root hash
    S get(byte[] rootHash, ID id);

    HexBytes getGenesisRoot();


    Trie<ID, S> getTrie();

    Trie<ID, S> getTrie(byte[] rootHash);

    Store<byte[], byte[]> getTrieStore();

    Backend createBackend(
            Header parent,
            long newBlockCreatedAt,
            boolean isStatic
    );


}
