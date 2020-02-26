package org.tdf.sunflower.vm.hosts;

import org.tdf.lotusvm.runtime.HostFunction;
import org.tdf.lotusvm.types.FunctionType;
import org.tdf.lotusvm.types.ValueType;

import java.util.ArrayList;
import java.util.Collections;

public class Parameters extends HostFunction {
    private byte[] data;

    public Parameters(byte[] data) {
        this.data = data;
        setType(new FunctionType(Collections.singletonList(ValueType.I32), new ArrayList<>()));
        setName("_parameters");
    }

    @Override
    public long[] execute(long... parameters) {
        putMemory((int)parameters[0], data);
        return new long[0];
    }
}
