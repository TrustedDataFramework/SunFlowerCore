package org.tdf.serialize;

import org.tdf.common.ChainCache;
import org.tdf.common.Chained;
import org.tdf.util.BufferUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Converter from byte array to ChainCache and vice versa
 *
 */
public class ChainCacheSerializerDeserializer<T extends Chained> implements SerializeDeserializer<ChainCache<T>> {
    private SerializeDeserializer<T> serializeDeserializer;

    public ChainCacheSerializerDeserializer(SerializeDeserializer<T> serializeDeserializer) {
        this.serializeDeserializer = serializeDeserializer;
    }

    @Override
    public byte[] serialize(ChainCache<T> chainCache) {
        List<T> all = chainCache.getAll();
        BufferUtil util = BufferUtil.newWriteOnly();
        util.putInt(all.size());
        for(int i = 0; i < all.size(); i++){
            util.putBytes(serializeDeserializer.serialize(all.get(i)));
        }
        return util.toByteArray();
    }

    @Override
    public ChainCache<T> deserialize(byte[] data) {
        BufferUtil util = BufferUtil.newReadOnly(data);
        List<T> all = new ArrayList<>();
        int size = util.getInt();
        for(int i = 0; i < size; i++){
            all.add(serializeDeserializer.deserialize(util.getBytes()));
        }
        return new ChainCache<>(all);
    }
}
