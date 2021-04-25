package org.tdf.common.store;

import com.google.common.primitives.Bytes;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.tdf.common.serialize.Codec;


@RequiredArgsConstructor
public class PrefixStore<K, V> implements Store<K, V> {
    private final Store<byte[], byte[]> contractStorage;
    private final byte[] prefix;
    private final Codec<K> kCodec;
    private final Codec<V> vCodec;

    @Override
    public V get(@NonNull K k) {
        byte[] v = contractStorage.get(
                addPrefix(k)
        );
        return (v == null || v.length == 0) ? null : vCodec.getDecoder().apply(v);
    }

    private byte[] addPrefix(K k) {
        byte[] encoded = kCodec.getEncoder().apply(k);
        return Bytes.concat(prefix, encoded);
    }

    @Override
    public void put(@NonNull K k, @NonNull V v) {
        contractStorage.put(addPrefix(k), vCodec.getEncoder().apply(v));
    }

    @Override
    public void remove(@NonNull K k) {
        contractStorage.remove(addPrefix(k));
    }

    @Override
    public void flush() {
        contractStorage.flush();
    }
}

