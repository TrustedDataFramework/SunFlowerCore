package org.tdf.common.trie;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.tdf.common.serialize.Codec;
import org.tdf.common.store.Store;

import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class ReadOnlyTrie<K, V> extends AbstractTrie<K, V> {
    protected AbstractTrie<K, V> delegate;

    public static <K, V> Trie<K, V> of(Trie<K, V> trie) {
        if (trie instanceof ReadOnlyTrie) return trie;
        if (trie.isDirty()) throw new UnsupportedOperationException();
        return new ReadOnlyTrie<>((AbstractTrie<K, V>) trie);
    }

    @Override
    public Trie<K, V> revert(byte[] rootHash, Store<byte[], byte[]> store) throws RuntimeException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Trie<K, V> revert(byte[] rootHash) throws RuntimeException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Trie<K, V> revert() {
        throw new UnsupportedOperationException();
    }

    @Override
    public byte[] commit() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<byte[]> dumpKeys() {
        return delegate.dumpKeys();
    }

    @Override
    public Map<byte[], byte[]> dump() {
        return delegate.dump();
    }

    @Override
    public byte[] getRootHash() throws RuntimeException {
        return delegate.getRootHash();
    }

    @Override
    public byte[] getNullHash() {
        return delegate.getNullHash();
    }

    @Override
    public boolean isDirty() {
        return delegate.isDirty();
    }

    @Override
    public V get(K k) {
        return delegate.get(k);
    }

    @Override
    public void put(K k, V v) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void remove(K k) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void flush() {
        throw new UnsupportedOperationException();
    }


    @Override
    public Map<byte[], byte[]> getProof(K k) {
        return delegate.getProof(k);
    }

    @Override
    public Codec<K> getKCodec() {
        return delegate.getKCodec();
    }

    @Override
    public Codec<V> getVCodec() {
        return delegate.getVCodec();
    }

    @Override
    public V getFromBytes(byte[] data) {
        return delegate.getFromBytes(data);
    }

    @Override
    public void putBytes(byte[] key, byte[] value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeBytes(byte[] data) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<byte[], byte[]> getProofInternal(byte[] key) {
        return delegate.getProofInternal(key);
    }

    @Override
    public void traverseInternal(BiFunction<byte[], byte[], Boolean> traverser) {
        delegate.traverseInternal(traverser);
    }

    @Override
    public Store<byte[], byte[]> getStore() {
        return delegate.getStore();
    }
}
