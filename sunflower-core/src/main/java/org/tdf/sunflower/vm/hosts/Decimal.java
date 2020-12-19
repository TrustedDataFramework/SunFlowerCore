package org.tdf.sunflower.vm.hosts;

import org.tdf.lotusvm.runtime.HostFunction;
import org.tdf.lotusvm.types.FunctionType;
import org.tdf.lotusvm.types.ValueType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Decimal {
    public List<HostFunction> getHelpers() {
        return Collections.singletonList(new DecimalHost());
    }

    private enum Type {
        ADD, SUB, MUL, DIV, COMPARE
    }

    private static class DecimalHost extends HostFunction {

        public DecimalHost() {
            setName("_decimal");
            setType(
                    new FunctionType(
                            Arrays.asList
                                    (ValueType.I32,
                                            ValueType.I32, ValueType.I32,
                                            ValueType.I32, ValueType.I32,
                                            ValueType.I64, ValueType.I64,
                                            ValueType.I64
                                    ),
                            Collections.singletonList(ValueType.I32)
                    )
            );
        }

        @Override
        public long execute(long... parameters) {
            String x = loadStringFromMemory((int) parameters[1], (int) parameters[2]);
            String y = loadStringFromMemory((int) parameters[3], (int) parameters[4]);
            Type t = Type.values()[(int) parameters[0]];
            int ptr = (int) parameters[5];
            String ret;
            switch (t) {
                case ADD:
                    ret = new BigDecimal(x).add(new BigDecimal(y)).toString();
                    break;
                case SUB:
                    ret = new BigDecimal(x).subtract(new BigDecimal(y)).toString();
                    break;
                case MUL:
                    ret = new BigDecimal(x).multiply(new BigDecimal(y)).toString();
                    break;
                case DIV:
                    ret = new BigDecimal(x).divide(new BigDecimal(y), (int) parameters[6], RoundingMode.UNNECESSARY).toString();
                    break;
                case COMPARE:
                    return new BigDecimal(x).compareTo(new BigDecimal(y));
                default:
                    throw new RuntimeException("unreachable");
            }
            if (parameters[7] != 0) {
                putStringIntoMemory(ptr, ret);
            }
            return ret.length();
        }
    }
}
