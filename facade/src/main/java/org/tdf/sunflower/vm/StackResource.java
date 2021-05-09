package org.tdf.sunflower.vm;

import org.tdf.lotusvm.runtime.StackProvider;

import java.io.Closeable;

public interface StackResource extends StackProvider, Closeable {
    @Override
    void close();
}
