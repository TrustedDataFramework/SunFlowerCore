package org.tdf.sunflower.state;

import org.tdf.common.store.Store;
import org.tdf.common.trie.Trie;
import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.types.Header;
import org.tdf.sunflower.vm.Backend;

import java.util.List;

/**
 * state storage
 *
 * @param <ID> identifier of state
 * @param <S>  state
 */
public interface StateTrie<ID, S> {
    // get an optional state at a root hash
    S get(HexBytes rootHash, ID id);

    // init genesis states
    HexBytes init(
        List<Account> alloc,
        List<BuiltinContract> bios,
        List<BuiltinContract> builtins
    );

    Trie<ID, S> getTrie();

    Trie<ID, S> getTrie(HexBytes rootHash);

    Store<byte[], byte[]> getTrieStore();

    default Backend createBackend(
        Header parent,
        Long newBlockCreatedAt,
        boolean isStatic
    ) {
        return createBackend(parent, parent.getStateRoot(), newBlockCreatedAt, isStatic);
    }

    Backend createBackend(
        Header parent,
        HexBytes root,
        Long newBlockCreatedAt,
        boolean isStatic
    );
}
