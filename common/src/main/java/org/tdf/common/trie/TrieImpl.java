package org.tdf.common.trie;

import com.github.salpadding.rlpstream.Rlp;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import org.tdf.common.serialize.Codec;
import org.tdf.common.store.ReadOnlyStore;
import org.tdf.common.store.Store;
import org.tdf.common.util.HexBytes;

import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;


// enhanced radix tree
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TrieImpl<K, V> extends AbstractTrie<K, V> {

    @Getter
    Store<byte[], byte[]> store;

    Codec<K> kCodec;
    Codec<V> vCodec;
    private Node root;

    static <K, V> TrieImpl<K, V> newInstance(
        @NonNull Store<byte[], byte[]> store,
        @NonNull Codec<K> keyCodec,
        @NonNull Codec<V> valueCodec
    ) {

        return new TrieImpl<>(
            store,
            keyCodec,
            valueCodec,
            null
        );
    }

    Codec<K> getKCodec() {
        return kCodec;
    }

    Codec<V> getVCodec() {
        return vCodec;
    }

    V getFromBytes(byte[] data) {
        if (data == null || data.length == 0) throw new IllegalArgumentException("key cannot be null");
        if (root == null) return null;
        byte[] v = root.get(TrieKey.fromNormal(data));
        return (v == null || v.length == 0) ? null : vCodec.getDecoder().apply(v);
    }

    void putBytes(byte[] key, byte[] value) {
        if (key == null || key.length == 0) throw new IllegalArgumentException("key cannot be null");
        if (value == null || value.length == 0) {
            removeBytes(key);
            return;
        }
        if (root == null) {
            root = Node.newLeaf(TrieKey.fromNormal(key), value);
            return;
        }
        root.insert(TrieKey.fromNormal(key), value, store);
    }

    void removeBytes(byte[] data) {
        if (data == null || data.length == 0) throw new IllegalArgumentException("key cannot be null");
        if (root == null) return;
        root = root.delete(TrieKey.fromNormal(data), store);
    }

    public void clear() {
        root = null;
    }

    public HexBytes commit() {
        if (root == null) return getNullHash();
        if (!root.isDirty()) return HexBytes.fromBytes(root.getHash());
        byte[] hash = Rlp.decodeBytes(this.root.commit(store, true));
        if (root.isDirty() || root.getHash() == null)
            throw new RuntimeException("unexpected error: still dirty after commit");
        return HexBytes.fromBytes(hash);
    }

    @Override
    public void flush() {
        store.flush();
    }

    @Override
    public TrieImpl<K, V> revert(@NonNull HexBytes rootHash, Store<byte[], byte[]> store) {
        if (rootHash.equals(getNullHash()))
            return new TrieImpl<>(store,
                kCodec, vCodec, null);
        byte[] v = store.get(rootHash.getBytes());
        if (v == null || v.length == 0) throw new RuntimeException("rollback failed, root hash not exists");
        return new TrieImpl<>(
            store, kCodec, vCodec,
            Node.fromRootHash(rootHash.getBytes(), ReadOnlyStore.of(store))
        );
    }

    public void traverseTrie(BiFunction<TrieKey, Node, Boolean> action) {
        if (root == null) return;
        root.traverse(TrieKey.EMPTY, action);
    }

    @Override
    public Set<HexBytes> dumpKeys() {
        if (isDirty()) throw new UnsupportedOperationException();
        DumpKeys dump = new DumpKeys();
        traverseTrie(dump);
        return dump.getKeys();
    }

    @Override
    public Map<HexBytes, HexBytes> dump() {
        if (isDirty()) throw new UnsupportedOperationException();
        Dump dump = new Dump();
        traverseTrie(dump);
        return dump.getPairs();
    }

    @Override
    public HexBytes getRootHash() throws RuntimeException {
        if (root == null) return getNullHash();
        if (root.isDirty() || root.getHash() == null)
            throw new RuntimeException("the trie is dirty or root hash is null");
        return HexBytes.fromBytes(root.getHash());
    }

    @Override
    public boolean isDirty() {
        return root != null && root.isDirty();
    }

    @Override
    public TrieImpl<K, V> revert(HexBytes rootHash) throws RuntimeException {
        return revert(rootHash, store);
    }

    @Override
    public TrieImpl<K, V> revert() {
        return new TrieImpl<>(
            store, kCodec, vCodec,
            null
        );
    }

    @Override
    void traverseInternal(BiFunction<byte[], byte[], Boolean> traverser) {
        traverseTrie((k, n) -> {
            if (n.getType() != Node.Type.EXTENSION && n.getValue() != null) {
                return traverser.apply(k.toNormal(), n.getValue());
            }
            return true;
        });
    }
}
