package org.tdf.common.trie;

import org.tdf.common.serialize.Codec;
import org.tdf.common.store.Store;
import org.tdf.rlp.RLPElement;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * https://medium.com/codechain/secure-tree-why-state-tries-key-is-256-bits-1276beb68485
 * <p>
 * That is, if there are new nodes added into, a modification of, or an attempt to read these two tries,
 * there must be a disk IO, and if possible, must pass by the least amount of nodes possible until reaching the leaf node.
 * For this reason, MPT allowed for the compressing of the 1-child branch node with the extension node.
 * However, only branch nodes with a single child can be compressed into an extension node.
 * Thus, if an attacker can maliciously create a branch node with two children, he/she can attack at a low cost.
 * Thus, the secure tree uses a keccak-256 hash value as its key, and prevents an attacker from creating a node at a location that he/she desires.
 *
 * @param <V> value type
 */
public class SecureTrie<K, V> extends AbstractTrie<K, V> {
    private AbstractTrie<K, V> delegate;

    private Function<byte[], byte[]> hashFunction;


    public SecureTrie(Trie<K, V> delegate, Function<byte[], byte[]> hashFunction) {
        this.delegate = (AbstractTrie<K, V>) delegate;
        this.hashFunction = hashFunction;
    }

    @Override
    public Trie<K, V> revert(byte[] rootHash, Store<byte[], byte[]> store) throws RuntimeException {
        return new SecureTrie<>(delegate.revert(rootHash, store), hashFunction);
    }

    @Override
    public Trie<K, V> revert(byte[] rootHash) throws RuntimeException {
        return new SecureTrie<>(delegate.revert(rootHash), hashFunction);
    }

    @Override
    public Trie<K, V> revert() {
        return new SecureTrie<>(delegate.revert(), hashFunction);
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
    public V getTrap() {
        return delegate.getTrap();
    }

    @Override
    public boolean isTrap(V v) {
        return delegate.isTrap(v);
    }


    @Override
    public void flush() {
        delegate.flush();
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
    public void traverse(BiFunction<? super K, ? super V, Boolean> traverser) {
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
    public Stream<Map.Entry<K, V>> stream() {
        throw new UnsupportedOperationException("not supported in secure trie");
    }

    @Override
    public RLPElement getProof(Collection<? extends K> k) {
        return delegate.getMerklePathInternal(
                k.stream().map(getKCodec().getEncoder())
                        .map(hashFunction).collect(Collectors.toList())
        );
    }

    @Override
    public Trie<K, V> revertToProof(RLPElement proof) {
        return new SecureTrie<>(
                delegate.revertToProof(proof),
                hashFunction
        );
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
        return delegate.getFromBytes(hashFunction.apply(data));
    }

    @Override
    public void putBytes(byte[] key, byte[] value) {
        delegate.putBytes(hashFunction.apply(key), value);
    }

    @Override
    public void removeBytes(byte[] data) {
        delegate.removeBytes(hashFunction.apply(data));
    }

    @Override
    public RLPElement getMerklePathInternal(Collection<? extends byte[]> bytes) {
        return delegate.getMerklePathInternal(bytes.stream().map(hashFunction).collect(Collectors.toList()));
    }
}
