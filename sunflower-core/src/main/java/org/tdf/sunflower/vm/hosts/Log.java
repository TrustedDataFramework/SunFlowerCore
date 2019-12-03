package org.tdf.sunflower.vm.hosts;

import org.tdf.lotusvm.runtime.HostFunction;
import org.tdf.lotusvm.types.FunctionType;
import org.tdf.lotusvm.types.ValueType;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;

public class Log extends HostFunction {
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
