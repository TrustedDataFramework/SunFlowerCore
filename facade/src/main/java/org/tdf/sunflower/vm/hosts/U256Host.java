package org.tdf.sunflower.vm.hosts;

import org.tdf.common.types.Uint256;
import org.tdf.lotusvm.runtime.HostFunction;
import org.tdf.lotusvm.types.FunctionType;
import org.tdf.lotusvm.types.ValueType;
import org.tdf.sunflower.vm.WBI;
import org.tdf.sunflower.vm.abi.WbiType;

import java.util.Arrays;
import java.util.Collections;

public class U256Host extends HostFunction {
    enum U256OP {
        ADD,
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
        Uint256 l = (Uint256) WBI.peek(getInstance(), (int) longs[1], WbiType.UINT_256);
        Uint256 r = (Uint256) WBI.peek(getInstance(), (int) longs[2], WbiType.UINT_256);
        Uint256 res;
        switch (op) {
            case ADD:
                res = l.uncheckedPlus(r);
                break;
            case SUB:
                res = l.uncheckedMinus(r);
                break;
            case MUL:
                res = l.uncheckedTimes(r);
                break;
            case DIV:
                res = l.uncheckedDiv(r);
                break;
            case MOD:
                res = l.uncheckedRem(r);
                break;
            default:
                throw new UnsupportedOperationException();
        }
        int offset = WBI.malloc(getInstance(), res);
        return Integer.toUnsignedLong(offset);
    }
}
