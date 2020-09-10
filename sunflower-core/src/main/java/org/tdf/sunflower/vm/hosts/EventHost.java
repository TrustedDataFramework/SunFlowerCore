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

import java.util.*;

public class EventHost extends HostFunction {
    private HexBytes address;
    private boolean readonly;
    @Getter
    private final List<Event> events;

    public EventHost(HexBytes address, boolean readonly) {
        this.address = address;
        setName("_event");
        setType(
                new FunctionType(
                        Arrays.asList
                                (ValueType.I64, ValueType.I64, ValueType.I64, ValueType.I64),
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
        String x = loadStringFromMemory((int) parameters[0], (int) parameters[1]);
        byte[] y = loadMemory((int) parameters[2], (int) parameters[3]);
        RLPList li = RLPElement.fromEncoded(y).asRLPList();
        WebSocket.broadcastEvent(address.getBytes(), x, li);
        events.add(new Event(x, li));
        return new long[0];
    }
}
