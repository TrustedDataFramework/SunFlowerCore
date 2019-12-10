package org.tdf.trie;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.tdf.serialize.RLPItem;
import org.tdf.store.CachedStore;
import org.tdf.store.ReadOnlyStore;
import org.tdf.store.Store;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import static org.tdf.trie.TrieKey.EMPTY;

// enhanced radix tree
@Builder(access = AccessLevel.PRIVATE)
@AllArgsConstructor
public class TrieImpl implements Trie {
    private static final byte[] EMPTY_BYTE_ARRAY = new byte[0];

    private Node root;

    HashFunction function;

    CachedStore<byte[]> cache;

    private TrieImpl(){}

    public TrieImpl(HashFunction function, Store<byte[], byte[]> store) {
        this.function = function;
        this.cache = new CachedStore<>(store);
    }

    public TrieImpl(HashFunction function, Store<byte[], byte[]> store, byte[] rootHash) {
        this.function = function;
        this.cache = new CachedStore<>(store);
        this.root = Node.fromEncoded(RLPItem.fromBytes(rootHash), new ReadOnlyStore<>(store));
    }

    @Override
    public Optional<byte[]> get(byte[] bytes) {
        if (root == null) return Optional.empty();
        return Optional.ofNullable(root.get(TrieKey.fromNormal(bytes)));
    }

    @Override
    public void put(byte[] bytes, byte[] bytes2) {
        if (bytes == null || bytes.length == 0) throw new IllegalArgumentException("key cannot be null");
        if (bytes2 == null || bytes2.length == 0) {
            remove(bytes);
            return;
        }
        if (root == null) {
            root = Node.newLeaf(TrieKey.fromNormal(bytes), bytes2);
            return;
        }
        root.insert(TrieKey.fromNormal(bytes), bytes2, cache);
    }

    @Override
    public void putIfAbsent(byte[] bytes, byte[] bytes2) {
        if (root != null && root.get(TrieKey.fromNormal(bytes)) != null) return;
        put(bytes, bytes2);
    }

    @Override
    public void remove(byte[] bytes) {
        if (root == null) return;
        root = root.delete(TrieKey.fromNormal(bytes), cache);
    }

    @Override
    public Set<byte[]> keySet() {
        if (root == null) return Collections.emptySet();
        ScanKeySet action = new ScanKeySet();
        root.traverse(EMPTY, action);
        return action.getBytes();
    }

    @Override
    public Collection<byte[]> values() {
        if (root == null) return Collections.emptySet();
        ScanValues action = new ScanValues();
        root.traverse(EMPTY, action);
        return action.getBytes();
    }

    @Override
    public boolean containsKey(byte[] bytes) {
        if (root == null) return false;
        return root.get(TrieKey.fromNormal(bytes)) != null;
    }

    @Override
    public int size() {
        return keySet().size();
    }

    @Override
    public boolean isEmpty() {
        return root != null;
    }

    @Override
    public void clear() {
        root = null;
    }


    public byte[] getRootHash() {
        if (root == null) return function.apply(RLPItem.NULL.getEncoded());
        if(!isDirty()) return root.getHash();
        return commit().getRootHash();
    }

    @Override
    public TrieImpl commit() {
        if(!root.isDirty()) return this;
        TrieImpl trie = builder()
                .function(function)
                .cache(cache.clone())
                .root(root).build();
        trie.root.encodeAndCommit(trie.function, trie.cache, true, true);
        return trie;
    }

    @Override
    public boolean flush() {
        if (root == null) return false;
        this.root.encodeAndCommit(function, cache, true, true);
        return this.cache.flush();
    }

    @Override
    public boolean isDirty() {
        return root != null && root.isDirty();
    }
}
