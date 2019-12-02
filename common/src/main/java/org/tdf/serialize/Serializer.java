package org.tdf.serialize;

/**
 * Converter from T to byte array
 *
 */
public interface Serializer<T> {
    byte[] serialize(T t);
}
