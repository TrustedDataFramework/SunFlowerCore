package org.tdf.common.trie;

import lombok.Getter;
import org.tdf.common.util.HexBytes;

import java.util.function.Function;

public class HashFunction implements Function<byte[], byte[]> {
    @Getter
    private final int size;

    private final Function<byte[], byte[]> delegate;

    public HashFunction(Function<byte[], byte[]> delegate) {
        this.delegate = delegate;
        this.size = delegate.apply(HexBytes.EMPTY_BYTES).length;
    }

    @Override
    public byte[] apply(byte[] bytes) {
        return delegate.apply(bytes);
    }
}
