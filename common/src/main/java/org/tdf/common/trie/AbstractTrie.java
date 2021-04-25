package org.tdf.common.trie;

import lombok.NonNull;
import org.tdf.common.serialize.Codec;
import org.tdf.common.store.Store;

import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

abstract class AbstractTrie<K, V> implements Trie<K, V> {
    abstract Codec<K> getKCodec();

    abstract Codec<V> getVCodec();

    public abstract Store<byte[], byte[]> getStore();

    abstract V getFromBytes(byte[] data);

    abstract void putBytes(byte[] key, byte[] value);

    abstract void removeBytes(byte[] data);

    abstract Map<byte[], byte[]> getProofInternal(byte[] key);

    @Override
    public void put(@NonNull K k, @NonNull V val) {
        putBytes(getKCodec().getEncoder().apply(k), getVCodec().getEncoder().apply(val));
    }

    @Override
    public V get(@NonNull K k) {
        byte[] data = getKCodec().getEncoder().apply(k);
        return getFromBytes(data);
    }

    @Override
    public void remove(@NonNull K k) {
        byte[] data = getKCodec().getEncoder().apply(k);
        removeBytes(data);
    }

    public void traverse(BiFunction<? super K, ? super V, Boolean> traverser) {
        traverseInternal((k, v) -> traverser.apply(getKCodec().getDecoder().apply(k), getVCodec().getDecoder().apply(v)));
    }

    public void traverseValue(Function<? super V, Boolean> traverser) {
        traverseInternal((k, v) -> traverser.apply(getVCodec().getDecoder().apply(v)));
    }

    abstract void traverseInternal(BiFunction<byte[], byte[], Boolean> traverser);


    @Override
    public Map<byte[], byte[]> getProof(K k) {
        return getProofInternal(getKCodec().getEncoder().apply(k));
    }
}
