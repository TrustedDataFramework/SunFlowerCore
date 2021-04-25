package org.tdf.sunflower.vm.hosts;

import org.tdf.common.util.HexBytes;
import org.tdf.lotusvm.runtime.HostFunction;
import org.tdf.lotusvm.types.FunctionType;
import org.tdf.lotusvm.types.ValueType;
import org.tdf.sunflower.vm.Backend;
import org.tdf.sunflower.vm.abi.AbiDataType;

import java.util.Arrays;
import java.util.Collections;

public class EventHost extends HostFunction {
    private final HexBytes address;
    private final Backend backend;
    private final boolean isStatic;
    public static final FunctionType FUNCTION_TYPE = new FunctionType(
            Arrays.asList
                    (ValueType.I64, ValueType.I64),
            Collections.emptyList()
    );

    public EventHost(Backend backend, HexBytes address, boolean isStatic) {
        super("_event", FUNCTION_TYPE);
        this.backend = backend;
        this.address = address;
        this.isStatic = isStatic;
    }

    @Override
    public long execute(long... parameters) {
        if (isStatic)
            return 0;
        String x = (String) WBI
                .peek(getInstance(), (int) parameters[0], AbiDataType.STRING);
        byte[] y = (byte[]) WBI
                .peek(getInstance(), (int) parameters[1], AbiDataType.BYTES);

        backend.onEvent(address, x, y);
        return 0;
    }
}
