package org.tdf.serialize;

import org.tdf.util.BufferUtil;
import org.tdf.util.LittleEndian;

import java.util.Collection;

/**
 *
 * Converter from Collection to byte array
 */
public class CollectionSerializer<T> implements Serializer<Collection<T>> {
    private Serializer<T> serializer;

    public CollectionSerializer(Serializer<T> serializer) {
        this.serializer = serializer;
    }

    @Override
    public byte[] serialize(Collection<T> data) {
        int size = data == null ? 0 : data.size();
        if (size == 0) {
            return LittleEndian.encodeInt32(0);
        }
        BufferUtil writer = BufferUtil.newWriteOnly();
        writer.putInt(size);
        for (T s : data) {
            writer.putBytes(serializer.serialize(s));
        }
        return writer.toByteArray();
    }
}
