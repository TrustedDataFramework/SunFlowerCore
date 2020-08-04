package org.tdf.common.store;

import com.google.common.primitives.Bytes;
import lombok.RequiredArgsConstructor;
import org.tdf.common.serialize.Codec;
import org.tdf.common.util.FastByteComparisons;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class PrefixStore<K, V> implements Store<K, V> {
    private final Store<byte[], byte[]> contractStorage;
    private final byte[] prefix;
    private final Codec<K, byte[]> kCodec;
    private final Codec<V, byte[]> vCodec;

    @Override
    public Optional<V> get(K k) {
        return contractStorage.get(
                addPrefix(k)
        ).map(vCodec.getDecoder());
    }

    private byte[] addPrefix(K k) {
        byte[] encoded = kCodec.getEncoder().apply(k);
        return Bytes.concat(prefix, encoded);
    }

    @Override
    public void put(K k, V v) {
        contractStorage.put(addPrefix(k), vCodec.getEncoder().apply(v));
    }

    @Override
    public void remove(K k) {
        contractStorage.remove(addPrefix(k));
    }

    @Override
    public void flush() {
        contractStorage.flush();
    }

    @Override
    public void clear() {
        List<byte[]> keys =
                contractStorage.keySet()
                        .stream()
                        .filter(this::isPrefix)
                        .collect(Collectors.toList());
        keys.forEach(contractStorage::remove);
    }

    private boolean isPrefix(byte[] x) {
        return x.length >= prefix.length &&
                FastByteComparisons.compareTo(x, 0, prefix.length, prefix, 0, prefix.length) == 0;
    }

    @Override
    public void traverse(BiFunction<? super K, ? super V, Boolean> biFunction) {
        for (Map.Entry<byte[], byte[]> entry : contractStorage.entrySet()) {
            byte[] key = entry.getKey();
            if (!isPrefix(key))
                continue;
            byte[] trim = Arrays.copyOfRange(key, prefix.length, key.length);
            K k = kCodec.getDecoder().apply(trim);
            boolean cont = biFunction.apply(k, vCodec.getDecoder().apply(entry.getValue()));
            if (!cont)
                return;
        }
    }
}

