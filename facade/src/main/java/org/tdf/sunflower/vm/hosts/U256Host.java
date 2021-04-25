package org.tdf.sunflower.vm.hosts;

import org.tdf.common.types.Uint256;
import org.tdf.lotusvm.runtime.HostFunction;
import org.tdf.lotusvm.types.FunctionType;
import org.tdf.lotusvm.types.ValueType;
import org.tdf.sunflower.vm.WBI;
import org.tdf.sunflower.vm.abi.AbiDataType;

import java.util.Arrays;
import java.util.Collections;

public class U256Host extends HostFunction {
    enum U256OP {
        SUM,
        SUB,
        MUL,
        DIV,
        MOD
    }

    public static final FunctionType FUNCTION_TYPE = new FunctionType(
            // offset, length, offset
            Arrays.asList(ValueType.I64, ValueType.I64, ValueType.I64),
            Collections.singletonList(ValueType.I64));

    public U256Host() {
        super("_u256", FUNCTION_TYPE);
    }

    @Override
    public long execute(long[] longs) {
        int i = (int) longs[0];
        U256OP op = U256OP.values()[i];
        Uint256 l = (Uint256) WBI.peek(getInstance(), (int) longs[1], AbiDataType.U256);
        Uint256 r = (Uint256) WBI.peek(getInstance(), (int) longs[2], AbiDataType.U256);
        Uint256 res;
        switch (op) {
            case SUM:
                res = l.add(r);
                break;
            case SUB:
                res = l.sub(r);
                break;
            case MUL:
                res = l.mul(r);
                break;
            case DIV:
                res = l.div(r);
                break;
            case MOD:
                res = l.mod(r);
                break;
            default:
                throw new UnsupportedOperationException();
        }
        int offset = WBI.malloc(getInstance(), res);
        return Integer.toUnsignedLong(offset);
    }
}
