package org.tdf.serialize;

/**
 * Converter from byte array to T
 *
 */
public interface Deserializer<T> {
    T deserialize(byte[] data);
}
