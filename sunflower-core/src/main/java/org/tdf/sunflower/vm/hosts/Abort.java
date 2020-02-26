package org.tdf.sunflower.vm.hosts;

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
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        byte[] mem = getInstance()
                .getMemory();
        for (int i = offset; ; i++) {
            if(mem[i] == 0 && mem[i+1] == 0) break;
            if(mem[i] == 0) continue;
            os.write(mem[i] & 0xff);
        }
        return new String(os.toByteArray(), StandardCharsets.UTF_8);
    }
}
