package org.tdf.sunflower.vm.hosts;

import org.tdf.crypto.HashFunctions;
import org.tdf.lotusvm.runtime.HostFunction;
import org.tdf.lotusvm.types.FunctionType;
import org.tdf.lotusvm.types.ValueType;

import java.util.ArrayList;
import java.util.Arrays;

public class Keccak256 extends HostFunction {
    public Keccak256() {
        setType(new FunctionType(
                // offset, length, offset
                Arrays.asList(ValueType.I32, ValueType.I32, ValueType.I32),
                new ArrayList<>()
        ));
        setName("_hash_keccak256");
    }

    @Override
    public long[] execute(long... parameters) {
        byte[] data = loadMemory((int) parameters[0], (int) parameters[1]);
        putMemory((int) parameters[2], HashFunctions.keccak256(data));
        return new long[0];
    }
}
