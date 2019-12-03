package org.tdf.serialize;

import org.tdf.util.BufferUtil;

import java.util.stream.Stream;

/**
 * Supper class for SetDeserializer
 *
 */
public class StreamDeserializer<T> implements Deserializer<Stream<T>>{
    private Deserializer<T> deserializer;

    StreamDeserializer(Deserializer<T> deserializer) {
        this.deserializer = deserializer;
    }

    public Stream<T> deserialize(byte[] data){
        BufferUtil util = BufferUtil.newReadOnly(data);
        int size = util.getInt();
        if(size == 0) return Stream.empty();
        Stream.Builder<T> builder = Stream.builder();
        for(int i = 0; i < size; i++){
            builder.accept(deserializer.deserialize(util.getBytes()));
        }
        return builder.build();
    }
}
