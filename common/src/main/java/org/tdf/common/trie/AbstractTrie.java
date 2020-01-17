package org.tdf.common.trie;

import lombok.NonNull;
import org.tdf.common.serialize.Codec;
import org.tdf.rlp.RLPElement;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

abstract class AbstractTrie<K, V> implements Trie<K, V>{
    abstract Codec<K, byte[]> getKCodec();
    abstract Codec<V, byte[]> getVCodec();

    abstract Optional<V> getFromBytes(byte[] data);
    abstract void putBytes(byte[] key, byte[] value);
    abstract void removeBytes(byte[] data);
    abstract RLPElement getMerklePathInternal(Collection<? extends byte[]> keys);

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


    @Override
    public RLPElement getProof(Collection<? extends K> keys) {
        return getMerklePathInternal(keys.stream().map(getKCodec().getEncoder()).collect(Collectors.toList()));
    }
}
