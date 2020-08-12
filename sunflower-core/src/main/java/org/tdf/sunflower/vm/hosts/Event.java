package org.tdf.sunflower.vm.hosts;

import org.tdf.common.util.HexBytes;
import org.tdf.lotusvm.runtime.HostFunction;
import org.tdf.lotusvm.types.FunctionType;
import org.tdf.lotusvm.types.ValueType;
import org.tdf.sunflower.facade.Message;
import org.tdf.sunflower.facade.MessageQueue;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Event extends HostFunction {
    private MessageQueue<String, Message> mq;
    private HexBytes address;

    public Event(MessageQueue<String, Message> mq, HexBytes address) {
        this.mq = mq;
        this.address = address;
        setName("_event");
        setType(
                new FunctionType(
                        Arrays.asList
                                (ValueType.I64,
                                        ValueType.I64, ValueType.I64,
                                        ValueType.I64, ValueType.I64
                                ),
                        Collections.singletonList(ValueType.I64)
                )
        );
    }

    @Override
    public long[] execute(long... parameters) {
        String x = loadStringFromMemory((int) parameters[1], (int) parameters[2]);
        byte[] y = loadMemory((int) parameters[3], (int) parameters[4]);
        Map<String, String> m = new HashMap<>();
        m.put("data", HexBytes.encode(y));
        mq.publish(address + ":" + x, m);
        return new long[]{0};
    }
}
