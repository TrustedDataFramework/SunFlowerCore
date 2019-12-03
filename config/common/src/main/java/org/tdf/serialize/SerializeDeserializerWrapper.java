package org.tdf.serialize;

/**
 *  Combine serializer and deserializer to serializeDeserializer
 *
 */
public class SerializeDeserializerWrapper<T> implements SerializeDeserializer<T>{
    private Serializer<? super T> serializer;
    private Deserializer<? extends T> deserializer;

    public SerializeDeserializerWrapper(Serializer<? super T> serializer, Deserializer<? extends T> deserializer) {
        this.serializer = serializer;
        this.deserializer = deserializer;
    }

    @Override
    public T deserialize(byte[] data) {
        return deserializer.deserialize(data);
    }

    @Override
    public byte[] serialize(T t) {
        return serializer.serialize(t);
    }
}
