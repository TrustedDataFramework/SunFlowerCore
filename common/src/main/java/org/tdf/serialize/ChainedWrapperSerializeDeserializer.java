package org.tdf.serialize;

import org.tdf.common.ChainedWrapper;
import org.tdf.common.HexBytes;
import org.tdf.util.BufferUtil;

/**
 * Converter from byte array to ChainedWrapper and vice versa
 *
 */
public class ChainedWrapperSerializeDeserializer<T> implements SerializeDeserializer<ChainedWrapper<T>> {
    private SerializeDeserializer<T> serializeDeserializer;

    public ChainedWrapperSerializeDeserializer(SerializeDeserializer<T> serializeDeserializer) {
        this.serializeDeserializer = serializeDeserializer;
    }

    @Override
    public byte[] serialize(ChainedWrapper<T> chainedWrapper) {
        BufferUtil util = BufferUtil.newWriteOnly();
        util.putBytes(chainedWrapper.getHashPrev().getBytes());
        util.putBytes(chainedWrapper.getHash().getBytes());
        util.putBytes(serializeDeserializer.serialize(chainedWrapper.get()));
        return util.toByteArray();
    }

    @Override
    public ChainedWrapper<T> deserialize(byte[] data) {
        BufferUtil util = BufferUtil.newReadOnly(data);
        HexBytes hashPrev = new HexBytes(util.getBytes());
        HexBytes hash = new HexBytes(util.getBytes());
        T t = serializeDeserializer.deserialize(util.getBytes());
        return new ChainedWrapper<>(hashPrev, hash, t);
    }
}
