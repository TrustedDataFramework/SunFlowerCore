package org.tdf.sunflower.vm.hosts;

import org.tdf.lotusvm.runtime.HostFunction;
import org.tdf.lotusvm.types.FunctionType;

public class Ret extends HostFunction {
    public static final

    public Ret(String name, FunctionType type) {
        super(name, type);
    }

    @Override
    public long execute(long[] longs) {
        return 0;
    }
}
