package org.tdf.common.store;

import com.github.salpadding.rlpstream.Rlp;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.tdf.common.serialize.Codec;
import org.tdf.common.util.ByteUtil;
import org.tdf.common.util.HexBytes;
import org.tdf.common.util.RLPUtil;

import java.util.*;


@RequiredArgsConstructor
public class PrefixStore<K, V> implements IterableStore<K, V> {
    private final Store<HexBytes, HexBytes> contractStorage;
    private final HexBytes prefix;
    private final Codec<K> kCodec;
    private final Codec<V> vCodec;

    @Override
    public V get(@NonNull K k) {
        byte[] encoded = kCodec.getEncoder().apply(k);
        HexBytes withPrefix = verifyAndPrefix(encoded);
        HexBytes v = contractStorage.get(
            withPrefix
        );
        return (v == null || v.size() == 0) ? null : vCodec.getDecoder().apply(v.getBytes());
    }

    private HexBytes verifyAndPrefix(byte[] key) {
        if (key.length == 0)
            throw new RuntimeException("invalid key, length = 0");
        return HexBytes.fromBytes(ByteUtil.merge(prefix.getBytes(), key));
    }

    @Override
    public void set(@NonNull K k, @NonNull V v) {
        byte[] encoded = kCodec.getEncoder().apply(k);
        HexBytes withPrefix = verifyAndPrefix(encoded);
        addKey(encoded);
        contractStorage.set(withPrefix, HexBytes.fromBytes(vCodec.getEncoder().apply(v)));
    }

    private TreeSet<HexBytes> keySet() {
        HexBytes keys = contractStorage.get(prefix);
        TreeSet<HexBytes> keySet = keys == null || keys.size() == 0 ?
            new TreeSet<>() :
            new TreeSet<>(
                Arrays.asList(RLPUtil.decode(keys, HexBytes[].class))
            );
        return keySet;
    }

    private void addKey(byte[] key) {
        TreeSet<HexBytes> keySet = keySet();
        keySet.add(HexBytes.fromBytes(key));
        contractStorage.set(prefix, HexBytes.fromBytes(Rlp.encode(keySet.toArray())));
    }

    private void removeKey(byte[] key) {
        TreeSet<HexBytes> keySet = keySet();
        keySet.remove(HexBytes.fromBytes(key));
        contractStorage.set(prefix, HexBytes.fromBytes(Rlp.encode(keySet.toArray())));
    }

    @Override
    public void remove(@NonNull K k) {
        byte[] encoded = kCodec.getEncoder().apply(k);
        HexBytes withPrefix = verifyAndPrefix(encoded);
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
                    contractStorage.get(
                        verifyAndPrefix(key.getBytes())
                    ).getBytes()
                )
            );
        }
        return map.entrySet().iterator();
    }
}

