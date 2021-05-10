package org.tdf.sunflower.vm;

import org.tdf.lotusvm.runtime.StackAllocator;

import java.io.Closeable;

public interface StackResource extends StackAllocator, Closeable {
    @Override
    void close();
}
