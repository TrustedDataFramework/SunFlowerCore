package org.tdf.trie;

import org.tdf.common.Store;
import org.tdf.serialize.RLPItem;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import static org.tdf.trie.TrieKey.EMPTY;

// enhanced radix tree
public class TrieImpl implements Trie {
    private static final byte[] EMPTY_BYTE_ARRAY = new byte[0];

    private Node root;

    HashFunction function;

    Store<byte[], byte[]> cache;

    public TrieImpl(HashFunction function, Store<byte[], byte[]> cache) {
        this.function = function;
        this.cache = cache;
    }

    public TrieImpl(HashFunction function, Store<byte[], byte[]> cache, byte[] rootHash) {
        this.function = function;
        this.cache = cache;
        this.root = Node.fromEncoded(RLPItem.fromBytes(rootHash), cache);
    }

    @Override
    public Optional<byte[]> get(byte[] bytes) {
        if (root == null) return Optional.empty();
        return Optional.ofNullable(root.get(TrieKey.fromNormal(bytes)));
    }

    @Override
    public void put(byte[] bytes, byte[] bytes2) {
        if (root == null) {
            root = Node.newLeaf(TrieKey.fromNormal(bytes), bytes2);
            return;
        }
        root.insert(TrieKey.fromNormal(bytes), bytes2);
    }

    @Override
    public void putIfAbsent(byte[] bytes, byte[] bytes2) {
        if (root != null && root.get(TrieKey.fromNormal(bytes)) != null) return;
        put(bytes, bytes2);
    }

    @Override
    public void remove(byte[] bytes) {
        if (root == null) return;
        root = root.delete(TrieKey.fromNormal(bytes));
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
        return root.encodeAndCommit(function, cache, true).getAsItem().get();
    }
}
