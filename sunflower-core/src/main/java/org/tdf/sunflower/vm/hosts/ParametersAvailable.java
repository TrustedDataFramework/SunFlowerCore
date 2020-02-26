package org.tdf.sunflower.vm.hosts;

import org.tdf.lotusvm.runtime.HostFunction;
import org.tdf.lotusvm.types.FunctionType;
import org.tdf.lotusvm.types.ValueType;

import java.util.Collections;

public class ParametersAvailable extends HostFunction{
    private final boolean available;
    public ParametersAvailable(boolean available){
        this.available = available;
        setName("_parameters_available");
        setType(new FunctionType(Collections.emptyList(), Collections.singletonList(ValueType.I64)));
    }

    @Override
    public long[] execute(long... longs) {
        return new long[]{available ? 1 : 0};
    }
}
