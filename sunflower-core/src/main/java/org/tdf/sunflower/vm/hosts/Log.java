package org.tdf.sunflower.vm.hosts;

import lombok.extern.slf4j.Slf4j;
import org.tdf.lotusvm.runtime.HostFunction;
import org.tdf.lotusvm.types.FunctionType;
import org.tdf.lotusvm.types.ValueType;
import org.tdf.sunflower.vm.abi.AbiDataType;

import java.util.Arrays;
import java.util.Collections;

@Slf4j(topic = "vm")
public class Log extends HostFunction {
    public Log() {
        setType(new FunctionType(
                // offset, length, offset
                Arrays.asList(ValueType.I64),
                Collections.emptyList()
        ));
        setName("_log");
    }

    @Override
    public long execute(long... parameters) {
        log.info(
                (String) WBI.peek(getInstance(), (int) parameters[0], AbiDataType.STRING)
        );
        return 0;
    }
}
