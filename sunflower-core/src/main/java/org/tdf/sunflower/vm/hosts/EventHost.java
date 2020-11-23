package org.tdf.sunflower.vm.hosts;

import lombok.Getter;
import org.tdf.common.util.HexBytes;
import org.tdf.lotusvm.runtime.HostFunction;
import org.tdf.lotusvm.types.FunctionType;
import org.tdf.lotusvm.types.ValueType;
import org.tdf.rlp.RLPElement;
import org.tdf.rlp.RLPList;
import org.tdf.sunflower.controller.WebSocket;
import org.tdf.sunflower.types.Event;
import org.tdf.sunflower.vm.abi.AbiDataType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class EventHost extends HostFunction {
    @Getter
    private final List<Event> events;
    private HexBytes address;
    private boolean readonly;

    public EventHost(HexBytes address, boolean readonly) {
        this.address = address;
        setName("_event");
        setType(
                new FunctionType(
                        Arrays.asList
                                (ValueType.I64, ValueType.I64),
                        Collections.emptyList()
                )
        );
        this.readonly = readonly;
        this.events = new ArrayList<>();
    }

    @Override
    public long[] execute(long... parameters) {
        if (readonly)
            throw new RuntimeException("cannot call event here");
        String x = (String) WBI
                    .peek(getInstance(), (int) parameters[0], AbiDataType.STRING);
        byte[] y = (byte[]) WBI
                .peek(getInstance(), (int) parameters[1], AbiDataType.BYTES);

        RLPList li = RLPElement.fromEncoded(y).asRLPList();
        WebSocket.broadcastEvent(address.getBytes(), x, li);
        events.add(new Event(x, li));
        return new long[0];
    }
}
