package org.tdf.sunflower.vm.hosts;

import org.tdf.common.util.BigEndian;
import org.tdf.common.util.LittleEndian;
import org.tdf.lotusvm.runtime.HostFunction;
import org.tdf.lotusvm.types.FunctionType;
import org.tdf.lotusvm.types.ValueType;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;

public class Abort extends HostFunction {
    public Abort() {
        setType(new FunctionType(
                Arrays.asList(
                        ValueType.I32,
                        ValueType.I32,
                        ValueType.I32,
                        ValueType.I32
                ),
                new ArrayList<>())
        );
        setName("abort");
    }

    @Override
    public long[] execute(long... parameters) {
        String message = loadString((int) parameters[0]);
        String filename = loadString((int) parameters[1]);
        throw new RuntimeException(String.format("%s %s error at line %d column %d",
                filename, message,
                parameters[2], parameters[3])
        );
    }

    private String loadString(int offset){
        int size = LittleEndian.decodeInt32(loadMemory(offset - 4, 4));
        byte[] mem = loadMemory(offset, size);
        byte[] be = new byte[mem.length];
        for(int i = 0; i < be.length / 2; i++){
            be[i * 2] = mem[i * 2 + 1];
            be[i * 2 + 1] = mem[i * 2];
        }
        return new String(be, StandardCharsets.UTF_16);
    }
}
