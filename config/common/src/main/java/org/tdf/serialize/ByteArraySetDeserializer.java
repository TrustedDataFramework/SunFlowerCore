package org.tdf.serialize;


import org.tdf.util.ByteArraySet;

import java.util.stream.Collectors;

/**
 * Converter from byte array to byte array set
 *
 */
public class ByteArraySetDeserializer implements Deserializer<ByteArraySet>{
    private StreamDeserializer<byte[]> deserializer = new StreamDeserializer<>(Serializers.IDENTITY);

    @Override
    public ByteArraySet deserialize(byte[] data) {
        return deserializer.deserialize(data).collect(Collectors.toCollection(ByteArraySet::new));
    }
}
