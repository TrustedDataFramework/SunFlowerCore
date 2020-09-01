package org.tdf.sunflower.vm.hosts;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.tdf.common.util.HexBytes;
import org.tdf.lotusvm.runtime.HostFunction;
import org.tdf.lotusvm.types.FunctionType;
import org.tdf.lotusvm.types.ValueType;
import org.tdf.sunflower.vm.abi.ContractCall;

import java.util.Arrays;
import java.util.Collections;

public class Call extends HostFunction {
    private ContractCall parent;
    private byte[] result;

    public Call(ContractCall parent) {
        this.parent = parent;
        setType(new FunctionType(
                // offset, length, offset
                Arrays.asList(
                        ValueType.I32, ValueType.I32, ValueType.I32, ValueType.I32,
                        ValueType.I32, ValueType.I32, ValueType.I32, ValueType.I32),
                Collections.singletonList(ValueType.I64)
        ));
        setName("_call");
    }

    @Override
    public long[] execute(long... longs) {
        if (longs[0] == 0) {
            byte[] addr = loadMemory((int) longs[1], (int) longs[2]);
            String method = loadStringFromMemory((int) longs[3], (int) longs[4]);
            byte[] parameters = loadMemory((int) longs[5], (int) longs[6]);
            ContractCall forked = parent.fork();
            this.result = forked.call(HexBytes.fromBytes(addr), method, parameters);
            return new long[]{this.result.length};
        }

        putMemory((int) longs[7], this.result);
        this.result = null;
        return new long[]{0};
    }
}
