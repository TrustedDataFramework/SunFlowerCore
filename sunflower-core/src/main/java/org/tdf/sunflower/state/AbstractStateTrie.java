package org.tdf.sunflower.state;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.tdf.common.store.DatabaseStore;
import org.tdf.common.store.Store;
import org.tdf.common.trie.ReadOnlyTrie;
import org.tdf.common.trie.Trie;
import org.tdf.common.util.HexBytes;


@Slf4j(topic = "trie")
public abstract class AbstractStateTrie<ID, S> implements StateTrie<ID, S> {
    abstract Store<byte[], byte[]> getDB();

    public S get(HexBytes rootHash, ID id) {
        return getTrieForReadOnly(rootHash).get(id);
    }


    public Trie<ID, S> getTrie(HexBytes rootHash) {
        return getTrie().revert(rootHash);
    }

    @SneakyThrows
    protected Trie<ID, S> getTrieForReadOnly(HexBytes rootHash) {
        return ReadOnlyTrie.of(getTrie(rootHash));
    }

}
