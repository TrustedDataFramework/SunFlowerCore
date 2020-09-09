package org.tdf.sunflower.vm.hosts;

import org.tdf.common.util.HexBytes;
import org.tdf.lotusvm.runtime.HostFunction;
import org.tdf.lotusvm.types.FunctionType;
import org.tdf.lotusvm.types.ValueType;
import org.tdf.sunflower.controller.WebSocket;
import org.tdf.sunflower.facade.Message;
import org.tdf.sunflower.facade.MessageQueue;

import java.util.Arrays;
import java.util.Collections;

public class Event extends HostFunction {
    private MessageQueue<String, Message> mq;
    private HexBytes address;
    private boolean readonly;
    public Event(HexBytes address, boolean readonly) {
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
    }

    @Override
    public long[] execute(long... parameters) {
        if(readonly)
            throw new RuntimeException("cannot call event here");
        String x = loadStringFromMemory((int) parameters[0], (int) parameters[1]);
        byte[] y = loadMemory((int) parameters[2], (int) parameters[3]);
        WebSocket.broadcastEvent(address.getBytes(), x, y);
        return new long[0];
    }
}
