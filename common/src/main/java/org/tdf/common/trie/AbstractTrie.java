package org.tdf.common.trie;

import lombok.NonNull;
import org.tdf.common.serialize.Codec;
import org.tdf.rlp.RLPElement;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

abstract class AbstractTrie<K, V> implements Trie<K, V>{
    abstract Codec<K, byte[]> getKCodec();
    abstract Codec<V, byte[]> getVCodec();

    abstract Optional<V> getFromBytes(byte[] data);
    abstract void putBytes(byte[] key, byte[] value);
    abstract void removeBytes(byte[] data);
    abstract Map<byte[], byte[]> getProofInternal(byte[] key);

    @Override
    public void put(@NonNull K k, @NonNull V val) {
        putBytes(getKCodec().getEncoder().apply(k), getVCodec().getEncoder().apply(val));
    }

    @Override
    public Optional<V> get(@NonNull K k) {
        byte[] data = getKCodec().getEncoder().apply(k);
        return getFromBytes(data);
    }

    @Override
    public void remove(@NonNull K k) {
        byte[] data = getKCodec().getEncoder().apply(k);
        removeBytes(data);
    }

    abstract void traverseInternal(BiFunction<byte[], byte[], Boolean> traverser);

    @Override
    public void traverse(BiFunction<? super K, ? super V, Boolean> traverser) {
        traverseInternal((k, v) -> traverser.apply(getKCodec().getDecoder().apply(k), getVCodec().getDecoder().apply(v)));
    }

    @Override
    public Map<byte[], byte[]> getProof(K k) {
        return getProofInternal(getKCodec().getEncoder().apply(k));
    }
}
