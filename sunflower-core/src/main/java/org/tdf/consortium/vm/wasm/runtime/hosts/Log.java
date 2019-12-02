package org.wisdom.consortium.vm.wasm.runtime.hosts;

import org.wisdom.consortium.vm.wasm.types.FunctionType;
import org.wisdom.consortium.vm.wasm.types.ValueType;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;

public class Log extends HostFunction{
    public Log() {
        setType(new FunctionType(
                // offset, length, offset
                Arrays.asList(ValueType.I32, ValueType.I32),
                new ArrayList<>()
        ));
        setName("_log");
    }

    @Override
    public long[] execute(long... parameters) {
        System.out.println(
                new String(getInstance().getMemory().loadN((int)parameters[0], (int)parameters[1]), StandardCharsets.UTF_8)
        );
        return new long[0];
    }
}
