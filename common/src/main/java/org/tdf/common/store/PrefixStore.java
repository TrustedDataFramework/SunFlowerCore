package org.tdf.common.store;

import com.google.common.primitives.Bytes;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.tdf.common.serialize.Codec;
import org.tdf.common.util.HexBytes;
import org.tdf.rlp.RLPCodec;

import java.util.*;


@RequiredArgsConstructor
public class PrefixStore<K, V> implements IterableStore<K, V> {
    private final Store<byte[], byte[]> contractStorage;
    private final byte[] prefix;
    private final Codec<K> kCodec;
    private final Codec<V> vCodec;

    @Override
    public V get(@NonNull K k) {
        byte[] encoded = kCodec.getEncoder().apply(k);
        byte[] withPrefix = verifyAndPrefix(encoded);
        byte[] v = contractStorage.get(
                withPrefix
        );
        return (v == null || v.length == 0) ? null : vCodec.getDecoder().apply(v);
    }

    private byte[] verifyAndPrefix(byte[] key) {
        if(key.length == 0)
            throw new RuntimeException("invalid key, length = 0");
        return Bytes.concat(prefix, key);
    }

    @Override
    public void put(@NonNull K k, @NonNull V v) {
        byte[] encoded = kCodec.getEncoder().apply(k);
        byte[] withPrefix = verifyAndPrefix(encoded);
        addKey(encoded);
        contractStorage.put(withPrefix, vCodec.getEncoder().apply(v));
    }

    private TreeSet<HexBytes> keySet() {
        byte[] keys = contractStorage.get(prefix);
        TreeSet<HexBytes> keySet = keys == null || keys.length == 0 ?
                new TreeSet<>() :
                new TreeSet<>(
                        Arrays.asList(RLPCodec.decode(keys, HexBytes[].class))
                );
        return keySet;
    }

    private void addKey(byte[] key) {
        TreeSet<HexBytes> keySet = keySet();
        keySet.add(HexBytes.fromBytes(key));
        contractStorage.put(prefix, RLPCodec.encode(keySet));
    }

    private void removeKey(byte[] key) {
        TreeSet<HexBytes> keySet = keySet();
        keySet.remove(HexBytes.fromBytes(key));
        contractStorage.put(prefix, RLPCodec.encode(keySet));
    }

    @Override
    public void remove(@NonNull K k) {
        byte[] encoded = kCodec.getEncoder().apply(k);
        byte[] withPrefix = verifyAndPrefix(encoded);
        removeKey(encoded);
        contractStorage.remove(withPrefix);
    }

    @Override
    public void flush() {
        contractStorage.flush();
    }

    @Override
    public Iterator<Map.Entry<K, V>> iterator() {
        Map<K, V> map = new HashMap<>();
        for (HexBytes key : keySet()) {
            map.put(
                    kCodec.getDecoder().apply(key.getBytes()),
                    vCodec.getDecoder().apply(
                            contractStorage.get(verifyAndPrefix(key.getBytes()))
                    )
            );
        }
        return map.entrySet().iterator();
    }
}

