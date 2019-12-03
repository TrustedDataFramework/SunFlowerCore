package org.tdf.sunflower.vm.hosts;

import org.tdf.lotusvm.runtime.HostFunction;
import org.tdf.lotusvm.types.FunctionType;

import java.util.ArrayList;

// sizeof host function
public class Noop extends HostFunction {
    public Noop() {
        setType(new FunctionType(new ArrayList<>(), new ArrayList<>()));
        setName("_noop");
    }

    @Override
    public long[] execute(long... parameters) {
        return new long[0];
    }
}
