package org.tdf.sunflower.vm.hosts;

import org.tdf.common.util.HexBytes;
import org.tdf.lotusvm.runtime.HostFunction;
import org.tdf.lotusvm.types.FunctionType;
import org.tdf.lotusvm.types.ValueType;
import org.tdf.rlp.RLPElement;
import org.tdf.rlp.RLPList;
import org.tdf.sunflower.vm.Backend;
import org.tdf.sunflower.vm.WBI;
import org.tdf.sunflower.vm.abi.WbiType;

import java.util.Arrays;
import java.util.Collections;

public class EventHost extends HostFunction {
    private final HexBytes address;
    private final Backend backend;


    public static final FunctionType FUNCTION_TYPE = new FunctionType(
        Arrays.asList
            (ValueType.I64, ValueType.I64),
        Collections.emptyList()
    );

    public EventHost(Backend backend, HexBytes address) {
        super("_event", FUNCTION_TYPE);
        this.backend = backend;
        this.address = address;

    }

    @Override
    public long execute(long... parameters) {
        String x = (String) WBI
            .peek(getInstance(), (int) parameters[0], WbiType.STRING);
        byte[] y = (byte[]) WBI
            .peek(getInstance(), (int) parameters[1], WbiType.BYTES);

        RLPList li = RLPElement.fromEncoded(y).asRLPList();
//        backend.onEvent(address, x, li);
        return 0;
    }
}
