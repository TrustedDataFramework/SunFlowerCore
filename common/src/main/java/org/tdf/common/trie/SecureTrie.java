package org.tdf.common.trie;

import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.tdf.common.store.Store;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * https://medium.com/codechain/secure-tree-why-state-tries-key-is-256-bits-1276beb68485
 *
 * That is, if there are new nodes added into, a modification of, or an attempt to read these two tries,
 * there must be a disk IO, and if possible, must pass by the least amount of nodes possible until reaching the leaf node.
 * For this reason, MPT allowed for the compressing of the 1-child branch node with the extension node.
 * However, only branch nodes with a single child can be compressed into an extension node.
 * Thus, if an attacker can maliciously create a branch node with two children, he/she can attack at a low cost.
 * Thus, the secure tree uses a keccak-256 hash value as its key, and prevents an attacker from creating a node at a location that he/she desires.
 *
 * @param <V> value type
 */
@AllArgsConstructor
public class SecureTrie<V> implements Trie<byte[], V>{
    private Trie<byte[], V> delegate;

    private Function<byte[], byte[]> hashFunction;

    @Override
    public Trie<byte[], V> revert(byte[] rootHash, Store<byte[], byte[]> store) throws RuntimeException {
        return delegate.revert(rootHash, store);
    }

    @Override
    public Trie<byte[], V> revert(byte[] rootHash) throws RuntimeException {
        return delegate.revert(rootHash);
    }

    @Override
    public Trie<byte[], V> revert() {
        return delegate.revert();
    }

    @Override
    public byte[] commit() {
        return delegate.commit();
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
    public Optional<V> get(@NonNull byte[] bytes) {
        return delegate.get(hashFunction.apply(bytes));
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
    public void put(@NonNull byte[] bytes, V v) {
        delegate.put(hashFunction.apply(bytes), v);
    }

    @Override
    public void putIfAbsent(@NonNull byte[] bytes, V v) {
        delegate.putIfAbsent(hashFunction.apply(bytes), v);
    }

    @Override
    public void remove(@NonNull byte[] bytes) {
        delegate.remove(hashFunction.apply(bytes));
    }

    @Override
    public void flush() {
        delegate.flush();
    }

    @Override
    public boolean containsKey(byte[] bytes) {
        return delegate.containsKey(hashFunction.apply(bytes));
    }

    @Override
    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    @Override
    public void clear() {
        delegate.clear();
    }

    @Override
    public void traverse(BiFunction<? super byte[], ? super V, Boolean> traverser) {
        throw new UnsupportedOperationException("not supported in secure trie");
    }

    @Override
    public void forEach(BiConsumer<? super byte[], ? super V> consumer) {
        throw new UnsupportedOperationException("not supported in secure trie");
    }

    @Override
    public Collection<V> values() {
        return delegate.values();
    }

    @Override
    public int size() {
        return delegate.size();
    }

    @Override
    public Stream<Map.Entry<byte[], V>> stream() {
        throw new UnsupportedOperationException("not supported in secure trie");
    }
}
