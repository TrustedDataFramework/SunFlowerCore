package org.tdf.common.trie;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.tdf.common.serialize.Codec;
import org.tdf.common.store.Store;
import org.tdf.rlp.RLPElement;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.stream.Stream;

@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class ReadOnlyTrie<K, V> extends AbstractTrie<K, V> {
    protected AbstractTrie<K, V> delegate;

    public static <K, V> Trie<K, V> of(AbstractTrie<K, V> trie) {
        if (trie instanceof ReadOnlyTrie) return trie;
        if (trie.isDirty()) throw new UnsupportedOperationException();
        return new ReadOnlyTrie<>(trie);
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
    public Optional<V> get(K k) {
        return delegate.get(k);
    }

    @Override
    public V getTrap() {
        return delegate.getTrap();
    }

    @Override
    public boolean isTrap(V v) {
        return delegate.isTrap(v);
    }

    @Override
    public void put(K k, V v) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void putIfAbsent(K k, V v) {
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
    public boolean containsKey(K k) {
        return delegate.containsKey(k);
    }

    @Override
    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void traverse(BiFunction<? super K, ? super V, Boolean> traverser) {
        delegate.traverse(traverser);
    }

    @Override
    public void forEach(BiConsumer<? super K, ? super V> consumer) {
        delegate.forEach(consumer);
    }

    @Override
    public Set<K> keySet() {
        return Collections.unmodifiableSet(delegate.keySet());
    }

    @Override
    public Collection<V> values() {
        return Collections.unmodifiableCollection(delegate.values());
    }

    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        return Collections.unmodifiableSet(delegate.entrySet());
    }

    @Override
    public int size() {
        return delegate.size();
    }

    @Override
    public Stream<Map.Entry<K, V>> stream() {
        return delegate.stream();
    }

    @Override
    public Map<K, V> asMap() {
        return Collections.unmodifiableMap(delegate.asMap());
    }


    @Override
    public RLPElement getProof(K k) {
        return delegate.getProof(k);
    }

    @Override
    public Trie<K, V> revertToProof(RLPElement proof) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Codec<K, byte[]> getKCodec() {
        return delegate.getKCodec();
    }

    @Override
    public Codec<V, byte[]> getVCodec() {
        return delegate.getVCodec();
    }

    @Override
    public Optional<V> getFromBytes(byte[] data) {
        return delegate.getFromBytes(data);
    }

    @Override
    public void putBytes(byte[] key, byte[] value) {
        delegate.putBytes(key, value);
    }

    @Override
    public void removeBytes(byte[] data) {
        delegate.removeBytes(data);
    }

    @Override
    public RLPElement getMerklePathInternal(byte[] bytes) {
        return delegate.getMerklePathInternal(bytes);
    }
}
