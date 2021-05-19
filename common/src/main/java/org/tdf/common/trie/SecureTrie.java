package org.tdf.common.trie;

import org.tdf.common.serialize.Codec;
import org.tdf.common.store.Store;
import org.tdf.common.util.HashUtil;
import org.tdf.common.util.HexBytes;

import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

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

    public SecureTrie(Trie<K, V> delegate) {
        this.delegate = (AbstractTrie<K, V>) delegate;
    }

    @Override
    public Trie<K, V> revert(HexBytes rootHash, Store<byte[], byte[]> store) {
        return new SecureTrie<>(delegate.revert(rootHash, store));
    }

    @Override
    public Trie<K, V> revert(HexBytes rootHash) {
        return new SecureTrie<>(delegate.revert(rootHash));
    }

    @Override
    public Trie<K, V> revert() {
        return new SecureTrie<>(delegate.revert());
    }

    @Override
    public HexBytes commit() {
        return delegate.commit();
    }

    @Override
    public Set<HexBytes> dumpKeys() {
        return delegate.dumpKeys();
    }

    @Override
    public Map<HexBytes, HexBytes> dump() {
        return delegate.dump();
    }

    @Override
    public HexBytes getRootHash() {
        return delegate.getRootHash();
    }

    @Override
    public boolean isDirty() {
        return delegate.isDirty();
    }


    @Override
    public void flush() {
        delegate.flush();
    }

    @Override
    public void traverse(BiFunction<? super K, ? super V, Boolean> traverser) {
        throw new UnsupportedOperationException("not supported in secure trie");
    }

    @Override
    public Store<byte[], byte[]> getStore() {
        return delegate.getStore();
    }

    @Override
    public void traverseValue(Function<? super V, Boolean> traverser) {
        traverseInternal((k, v) -> traverser.apply(getVCodec().getDecoder().apply(v)));
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
        return delegate.getFromBytes(HashUtil.sha3(data));
    }

    @Override
    public void putBytes(byte[] key, byte[] value) {
        delegate.putBytes(HashUtil.sha3(key), value);
    }

    @Override
    public void removeBytes(byte[] data) {
        delegate.removeBytes(HashUtil.sha3(data));
    }

    @Override
    void traverseInternal(BiFunction<byte[], byte[], Boolean> traverser) {
        delegate.traverseInternal(traverser);
    }

    public class ValueOnlyEntry implements Map.Entry<K, V> {
        private V value;

        public ValueOnlyEntry(V value) {
            this.value = value;
        }

        @Override
        public K getKey() {
            throw new UnsupportedOperationException();
        }

        @Override
        public V getValue() {
            return value;
        }

        @Override
        public V setValue(V value) {
            throw new UnsupportedOperationException();
        }
    }
}
