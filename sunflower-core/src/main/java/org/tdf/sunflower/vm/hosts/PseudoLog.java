package org.tdf.sunflower.vm.hosts;

import org.tdf.lotusvm.runtime.HostFunction;
import org.tdf.lotusvm.types.FunctionType;
import org.tdf.lotusvm.types.ValueType;

import java.util.ArrayList;
import java.util.Arrays;

public class PseudoLog extends HostFunction {
    public PseudoLog() {
        setType(new FunctionType(
                // offset, length, offset
                Arrays.asList(ValueType.I32, ValueType.I32),
                new ArrayList<>()
        ));
        setName("_log");
    }

    @Override
    public long[] execute(long... parameters) {
        return new long[0];
    }
}
