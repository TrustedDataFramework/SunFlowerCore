package org.tdf.sunflower.vm.hosts;

import org.tdf.lotusvm.runtime.HostFunction;
import org.tdf.lotusvm.types.FunctionType;
import org.tdf.lotusvm.types.ValueType;

import java.util.ArrayList;
import java.util.Arrays;

public class Abort extends HostFunction {
    public Abort() {
        setType(new FunctionType(
                Arrays.asList(
                        ValueType.I32,
                        ValueType.I32,
                        ValueType.I32,
                        ValueType.I32
                ),
                new ArrayList<>())
        );
        setName("abort");
    }

    @Override
    public long[] execute(long... parameters) {
        throw new RuntimeException(String.format("error at line %d column %d", parameters[2], parameters[3]));
    }
}
