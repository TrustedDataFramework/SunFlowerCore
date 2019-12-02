package org.wisdom.consortium.vm.wasm.runtime.hosts;

import org.wisdom.consortium.vm.wasm.types.FunctionType;
import org.wisdom.consortium.vm.wasm.types.ValueType;

import java.util.ArrayList;
import java.util.Collections;

public class PayloadLength extends HostFunction {
    private byte[] data;

    public PayloadLength(byte[] data) {
        this.data = data;
        setType(new FunctionType(
                new ArrayList<>(),
                Collections.singletonList(ValueType.I32)
        ));
        setName("_payload_len");
    }

    @Override
    public long[] execute(long... parameters) {
        return new long[]{data.length};
    }
}
