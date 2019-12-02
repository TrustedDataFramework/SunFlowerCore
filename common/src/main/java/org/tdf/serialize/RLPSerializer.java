package org.tdf.serialize;

/**
 * The RLP Serializer supports byte[], String, HexBytes, byte, int, long and Collection of then
 * use @RLP annotation to serialize your class, the order
 */
public class RLPSerializer implements Serializer<Object> {

    public static final Serializer<Object> SERIALIZER = new RLPSerializer();

    @Override
    public byte[] serialize(Object t) {
        return RLPElement.encode(t).getEncoded();
    }
}

