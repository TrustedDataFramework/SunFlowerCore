package org.wisdom.consortium.vm.wasm.runtime.hosts;

import org.wisdom.consortium.vm.wasm.types.FunctionType;
import org.wisdom.consortium.vm.wasm.types.ValueType;

import java.util.ArrayList;
import java.util.Collections;

public class Payload extends HostFunction{
    private byte[] data;

    public Payload(byte[] data) {
        this.data = data;
        setType(new FunctionType(Collections.singletonList(ValueType.I32), new ArrayList<>()));
        setName("_payload");
    }

    @Override
    public long[] execute(long... parameters) {
        getInstance().getMemory().put((int)parameters[0], data);
        return new long[0];
    }
}
