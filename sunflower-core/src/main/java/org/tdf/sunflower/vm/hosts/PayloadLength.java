package org.tdf.sunflower.vm.hosts;
import org.tdf.lotusvm.runtime.HostFunction;
import org.tdf.lotusvm.types.FunctionType;
import org.tdf.lotusvm.types.ValueType;

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
