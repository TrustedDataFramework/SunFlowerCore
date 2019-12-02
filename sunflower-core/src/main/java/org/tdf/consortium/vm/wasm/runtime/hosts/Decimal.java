package org.wisdom.consortium.vm.wasm.runtime.hosts;

import org.wisdom.consortium.vm.wasm.runtime.ModuleInstance;
import org.wisdom.consortium.vm.wasm.types.FunctionType;
import org.wisdom.consortium.vm.wasm.types.ValueType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Decimal {

    private String result;

    public void requireResult(){
        if(result == null) throw new RuntimeException("non result found");
    }

    public List<HostFunction> getHelpers() {
        return Arrays.asList(
                new DecimalResult(this),
                new DecimalAdd(this),
                new DecimalSub(this),
                new DecimalMul(this),
                new DecimalStrictDiv(this),
                new DecimalDiv(this),
                new DecimalCompare()
        );
    }

    private static class DecimalResult extends HostFunction {
        private Decimal decimal;
        public DecimalResult(Decimal decimal) {
            this.decimal = decimal;
            setName("_decimal_result");
            setType(
                    new FunctionType(
                            Arrays.asList(ValueType.I32),
                            new ArrayList<>()
                    )
            );
        }

        @Override
        public long[] execute(long... parameters) {
            decimal.requireResult();
            getInstance().getMemory().putString((int) parameters[0], decimal.result);
            return new long[0];
        }
    }

    private static class DecimalAdd extends HostFunction {
        Decimal decimal;
        public DecimalAdd(Decimal decimal) {
            this.decimal = decimal;
            setName("_decimal_add");
            setType(
                    new FunctionType(
                            Arrays.asList(ValueType.I32, ValueType.I32, ValueType.I32, ValueType.I32),
                            Arrays.asList(ValueType.I32)
                    )
            );
        }

        @Override
        public long[] execute(long... parameters) {
            ModuleInstance instance = getInstance();
            String x = instance.getMemory().loadString((int) parameters[0], (int) parameters[1]);
            String y = instance.getMemory().loadString((int) parameters[2], (int) parameters[3]);
            String z = new BigDecimal(x).add(new BigDecimal(y)).toString();
            decimal.result = z;
            return new long[]{z.getBytes(StandardCharsets.UTF_8).length};
        }
    }

    private static class DecimalSub extends HostFunction {
        Decimal decimal;
        public DecimalSub(Decimal decimal) {
            this.decimal = decimal;
            setName("_decimal_sub");
            setType(
                    new FunctionType(
                            Arrays.asList(ValueType.I32, ValueType.I32, ValueType.I32, ValueType.I32),
                            Arrays.asList(ValueType.I32)
                    )
            );
        }

        @Override
        public long[] execute(long... parameters) {
            ModuleInstance instance = getInstance();
            String x = instance.getMemory().loadString((int) parameters[0], (int) parameters[1]);
            String y = instance.getMemory().loadString((int) parameters[2], (int) parameters[3]);
            String z = new BigDecimal(x).subtract(new BigDecimal(y)).toString();
            decimal.result = z;
            return new long[]{z.getBytes(StandardCharsets.UTF_8).length};
        }
    }

    private static class DecimalMul extends HostFunction {
        Decimal decimal;
        public DecimalMul(Decimal decimal) {
            this.decimal = decimal;
            setName("_decimal_mul");
            setType(
                    new FunctionType(
                            Arrays.asList(ValueType.I32, ValueType.I32, ValueType.I32, ValueType.I32),
                            Arrays.asList(ValueType.I32)
                    )
            );
        }

        @Override
        public long[] execute(long... parameters) {
            ModuleInstance instance = getInstance();
            String x = instance.getMemory().loadString((int) parameters[0], (int) parameters[1]);
            String y = instance.getMemory().loadString((int) parameters[2], (int) parameters[3]);
            String z = new BigDecimal(x).multiply(new BigDecimal(y)).toString();
            decimal.result = z;
            return new long[]{z.getBytes(StandardCharsets.UTF_8).length};
        }
    }

    private static class DecimalStrictDiv extends HostFunction {
        Decimal decimal;
        public DecimalStrictDiv(Decimal decimal) {
            this.decimal = decimal;
            setName("_decimal_strict_div");
            setType(
                    new FunctionType(
                            Arrays.asList(ValueType.I32, ValueType.I32, ValueType.I32, ValueType.I32),
                            Arrays.asList(ValueType.I32)
                    )
            );
        }

        @Override
        public long[] execute(long... parameters) {
            ModuleInstance instance = getInstance();
            String x = instance.getMemory().loadString((int) parameters[0], (int) parameters[1]);
            String y = instance.getMemory().loadString((int) parameters[2], (int) parameters[3]);
            BigDecimal z = new BigDecimal(x).divide(new BigDecimal(y), 1, RoundingMode.HALF_UP);
            if (new BigDecimal(z.intValue()).compareTo(z) != 0) {
                throw new RuntimeException(String.format("%s Strict div %s is noninteger", x, y));
            }
            decimal.result = new BigDecimal(x).divide(new BigDecimal(y), 0, RoundingMode.HALF_UP).toString();
            return new long[]{z.toString().getBytes(StandardCharsets.UTF_8).length};
        }
    }

    private static class DecimalDiv extends HostFunction {
        Decimal decimal;
        public DecimalDiv(Decimal decimal) {
            this.decimal = decimal;
            setName("_decimal_div");
            setType(
                    new FunctionType(
                            Arrays.asList(ValueType.I32, ValueType.I32, ValueType.I32, ValueType.I32, ValueType.I32),
                            Arrays.asList(ValueType.I32)
                    )
            );
        }

        @Override
        public long[] execute(long... parameters) {
            ModuleInstance instance = getInstance();
            String x = instance.getMemory().loadString((int) parameters[0], (int) parameters[1]);
            String y = instance.getMemory().loadString((int) parameters[2], (int) parameters[3]);
            int scale = (int) parameters[4];
            String z = new BigDecimal(x).divide(new BigDecimal(y), scale, RoundingMode.HALF_UP).toString();
            decimal.result = z;
            return new long[]{z.getBytes(StandardCharsets.UTF_8).length};
        }
    }

    private static class DecimalCompare extends HostFunction {
        public DecimalCompare() {
            setName("_decimal_compare_to");
            setType(
                    new FunctionType(
                            Arrays.asList(ValueType.I32, ValueType.I32, ValueType.I32, ValueType.I32),
                            Collections.singletonList(ValueType.I32)
                    )
            );
        }


        @Override
        public long[] execute(long... parameters) {
            ModuleInstance instance = getInstance();
            String x = instance.getMemory().loadString((int) parameters[0], (int) parameters[1]);
            String y = instance.getMemory().loadString((int) parameters[2], (int) parameters[3]);
            return new long[]{new BigDecimal(x).compareTo(new BigDecimal(y))};
        }
    }
}
