package org.tdf.serialize;

import java.util.HashSet;
import java.util.stream.Collectors;

/**
 * Converter from byte array to HashSet
 *
 */
public class HashSetDeserializer<T>  implements Deserializer<HashSet<T>>{
    private StreamDeserializer<T> deserializer;

    public HashSetDeserializer(Deserializer<T> deserializer){
        this.deserializer = new StreamDeserializer<>(deserializer);
    }

    @Override
    public HashSet<T> deserialize(byte[] data) {
        return deserializer.deserialize(data).collect(Collectors.toCollection(HashSet::new));
    }
}
