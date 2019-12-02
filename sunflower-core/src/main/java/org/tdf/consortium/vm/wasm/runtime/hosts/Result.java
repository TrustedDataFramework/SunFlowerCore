package org.wisdom.consortium.vm.wasm.runtime.hosts;

import com.google.common.primitives.Bytes;
import lombok.Getter;
import org.wisdom.consortium.vm.wasm.types.FunctionType;
import org.wisdom.consortium.vm.wasm.types.ValueType;

import java.util.ArrayList;
import java.util.Arrays;

public class Result extends HostFunction{
    @Getter
    private byte[] data = new byte[0];

    public Result(){
        setType(
                new FunctionType(Arrays.asList(ValueType.I32, ValueType.I32), new ArrayList<>())
        );
        setName("_result");
    }

    @Override
    public long[] execute(long... parameters) {
        this.data = Bytes.concat(data, getInstance().getMemory().loadN((int)parameters[0], (int)parameters[1]));
        return new long[0];
    }
}
