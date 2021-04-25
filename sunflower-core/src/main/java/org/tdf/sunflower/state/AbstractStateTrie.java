package org.tdf.sunflower.state;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.tdf.common.store.DatabaseStore;
import org.tdf.common.trie.ReadOnlyTrie;
import org.tdf.common.trie.Trie;


@Slf4j(topic = "trie")
public abstract class AbstractStateTrie<ID, S> implements StateTrie<ID, S> {
    abstract DatabaseStore getDB();

    public S get(byte[] rootHash, ID id) {
        return getTrieForReadOnly(rootHash).get(id);
    }


    public Trie<ID, S> getTrie(byte[] rootHash) {
        return getTrie().revert(rootHash);
    }

    @SneakyThrows
    protected Trie<ID, S> getTrieForReadOnly(byte[] rootHash) {
        return ReadOnlyTrie.of(getTrie(rootHash));
    }

}
