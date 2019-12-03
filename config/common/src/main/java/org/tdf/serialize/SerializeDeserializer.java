package org.tdf.serialize;

/**
 * Converter from one type to byte array and vice versa
 *
 */
public interface SerializeDeserializer<T> extends Serializer<T>, Deserializer<T>{
}
