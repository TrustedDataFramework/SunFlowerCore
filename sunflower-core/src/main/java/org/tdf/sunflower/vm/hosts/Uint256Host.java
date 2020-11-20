package org.tdf.sunflower.vm.hosts;

import org.tdf.common.types.Uint256;
import org.tdf.lotusvm.runtime.HostFunction;
import org.tdf.lotusvm.types.FunctionType;
import org.tdf.lotusvm.types.ValueType;
import org.tdf.sunflower.vm.abi.AbiDataType;

import java.util.Arrays;
import java.util.Collections;

public class Uint256Host extends HostFunction {
    public Uint256Host() {
        setType(
                new FunctionType(
                        Arrays.asList(
                                ValueType.I64,
                                ValueType.I64, ValueType.I64
                        ),
                        Collections.singletonList(ValueType.I64)
                )
        );
        setName("_u256");
    }

    @Override
    public long[] execute(long... longs) {
        Type t = Type.values()[(int) longs[0]];
        Uint256 ret;
        switch (t) {
            case ADD:
                ret = getX(longs).add(getY(longs));
                break;
            case SUB:
                ret = getX(longs).sub(getY(longs));
                break;
            case MUL:
                ret = getX(longs).mul(getY(longs));
                break;
            case DIV:
                ret = getX(longs).div(getY(longs));
                break;
            case MOD:
                ret = getX(longs).mod(getY(longs));
                break;
            default:
                throw new RuntimeException("unreachable");
        }
        int offset = WasmBlockChainInterface.malloc(getInstance(), ret);
        return new long[]{offset};
    }

    private Uint256 getX(long... longs) {
        return (Uint256) WasmBlockChainInterface.peek(getInstance(), (int) longs[1], AbiDataType.U256);
    }

    private Uint256 getY(long... longs) {
        return (Uint256) WasmBlockChainInterface.peek(getInstance(), (int) longs[2], AbiDataType.U256);
    }

    enum Type {
        ADD,
        SUB,
        MUL,
        DIV,
        MOD
    }
}
