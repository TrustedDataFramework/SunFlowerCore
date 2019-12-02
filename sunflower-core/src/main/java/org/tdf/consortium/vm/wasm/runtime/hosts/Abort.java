package org.wisdom.consortium.vm.wasm.runtime.hosts;

import org.wisdom.consortium.vm.wasm.types.FunctionType;
import org.wisdom.consortium.vm.wasm.types.ValueType;

import java.util.ArrayList;
import java.util.Arrays;

public class Abort extends HostFunction{
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
