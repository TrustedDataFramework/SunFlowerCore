package org.tdf.sunflower.vm.hosts;

import org.tdf.common.util.HexBytes;
import org.tdf.lotusvm.runtime.HostFunction;
import org.tdf.lotusvm.types.FunctionType;
import org.tdf.lotusvm.types.ValueType;
import org.tdf.sunflower.vm.abi.ContractCall;

import java.util.Arrays;
import java.util.Collections;

public class Reflect extends HostFunction {
    private final ContractCall parent;
    private byte[] result;
    private byte[] createdAddress;

    enum Type {
        CALL_WITHOUT_PUT, // call without put into memory
        CALL_WITH_PUT, // call and put into memory
        CREATE_WITHOUT_PUT,
        CREATE_WITH_PUT
    }

    public Reflect(ContractCall parent) {
        this.parent = parent;
        setType(new FunctionType(
                // offset, length, offset
                Arrays.asList(
                        ValueType.I64, ValueType.I64, ValueType.I64, ValueType.I64,
                        ValueType.I64, ValueType.I64, ValueType.I64, ValueType.I64,
                        ValueType.I64
                ),
                Collections.singletonList(ValueType.I64)
        ));
        setName("_reflect");
    }

    @Override
    public long[] execute(long... longs) {
        Type t = Type.values()[(int) longs[0]];
        byte[] data = null;
        long ret = 0;
        boolean put = false;
        switch (t) {
            case CALL_WITHOUT_PUT:
            case CALL_WITH_PUT: {
                if (t == Type.CALL_WITH_PUT){
                    ret = this.result.length;
                    data = this.result;
                    this.result = null;
                    put = true;
                    break;
                }
                byte[] addr = loadMemory((int) longs[1], (int) longs[2]);
                String method = loadStringFromMemory((int) longs[3], (int) longs[4]);
                if ("init".equals(method))
                    throw new RuntimeException("cannot call constructor");
                byte[] parameters = loadMemory((int) longs[5], (int) longs[6]);
                long amount = longs[7];
                ContractCall forked = parent.fork();
                this.result = forked.call(HexBytes.fromBytes(addr), method, parameters, amount);
                ret = this.result.length;
                break;
            }
            case CREATE_WITHOUT_PUT:
            case CREATE_WITH_PUT: {
                if (t == Type.CREATE_WITH_PUT){
                    ret = this.createdAddress.length;
                    data = this.createdAddress;
                    this.createdAddress = null;
                    put = true;
                    break;
                }
                byte[] binary = loadMemory((int) longs[1], (int) longs[2]);
                byte[] parameters = loadMemory((int) longs[3], (int) longs[4]);
                long amount = longs[7];
                ContractCall forked = parent.fork();
                this.createdAddress = forked.call(HexBytes.fromBytes(binary), "init", parameters, amount);
                break;
            }
        }

        if (put)
            putMemory((int) longs[8], data);
        return new long[]{ret};
    }
}
