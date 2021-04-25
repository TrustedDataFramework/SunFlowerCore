package org.tdf.sunflower.vm.hosts;

import lombok.extern.slf4j.Slf4j;
import org.tdf.lotusvm.runtime.HostFunction;
import org.tdf.lotusvm.types.FunctionType;
import org.tdf.lotusvm.types.ValueType;
import org.tdf.sunflower.vm.WBI;
import org.tdf.sunflower.vm.abi.AbiDataType;

import java.util.Collections;

@Slf4j(topic = "vm")
public class Log extends HostFunction {
    public static final FunctionType FUNCTION_TYPE = new FunctionType(
            // offset, length, offset
            Collections.singletonList(ValueType.I64),
            Collections.emptyList()
    );

    public Log() {
        super("_log", FUNCTION_TYPE);
    }

    @Override
    public long execute(long... parameters) {
        log.info(
                (String) WBI.peek(getInstance(), (int) parameters[0], AbiDataType.STRING)
        );
        return 0;
    }
}
