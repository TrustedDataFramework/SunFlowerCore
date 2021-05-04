package org.tdf.sunflower.vm.hosts;

import org.tdf.lotusvm.runtime.HostFunction;
import org.tdf.lotusvm.types.FunctionType;
import org.tdf.lotusvm.types.ValueType;
import org.tdf.sunflower.vm.Backend;
import org.tdf.sunflower.vm.CallData;
import org.tdf.sunflower.vm.WBI;

import java.util.Arrays;
import java.util.Collections;

import static org.tdf.sunflower.vm.hosts.ContextHost.ContextType.*;


public class ContextHost extends HostFunction {
    private final Backend backend;
    private final CallData callData;
    public static final FunctionType FUNCTION_TYPE = new FunctionType(Arrays.asList(ValueType.I64, ValueType.I64),
            Collections.singletonList(ValueType.I64));

    public ContextHost(
            Backend backend,
            CallData callData
    ) {
        super("_context", FUNCTION_TYPE);
        this.backend = backend;
        this.callData = callData;
    }

    @Override
    public long execute(long... parameters) {
        long type = parameters[0];

        switch ((int) type) {
            case THIS_ADDRESS: {
                return WBI.mallocAddress(getInstance(), callData.getTo());
            }
            case MSG_SENDER: {
                return WBI.mallocAddress(getInstance(), callData.getCaller());
            }
            case MSG_VALUE: {
                return
                        WBI.malloc(getInstance(), callData.getValue());
            }
            default:
                throw new RuntimeException("unexpected type " + type);
        }
    }

    public static class ContextType {
        public static final int THIS_ADDRESS = 0x644836c2;
        public static final int MSG_SENDER = 0xb2f2618c;
        public static final int MSG_VALUE = 0x6db8129b;
    }

}
