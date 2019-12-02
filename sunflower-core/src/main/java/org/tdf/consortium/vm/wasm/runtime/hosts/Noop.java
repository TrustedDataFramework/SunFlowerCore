package org.wisdom.consortium.vm.wasm.runtime.hosts;

import org.wisdom.consortium.vm.wasm.types.FunctionType;

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
