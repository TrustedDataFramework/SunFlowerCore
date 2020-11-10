package org.tdf.sunflower.vm.hosts;

import org.tdf.crypto.CryptoHelpers;
import org.tdf.gmhelper.SM3Util;
import org.tdf.lotusvm.runtime.HostFunction;
import org.tdf.lotusvm.types.FunctionType;
import org.tdf.lotusvm.types.ValueType;

import java.util.Arrays;
import java.util.Collections;

public class HashHost extends HostFunction {
    public HashHost() {
        setType(new FunctionType(
                // offset, length, offset
                Arrays.asList(ValueType.I64, ValueType.I64, ValueType.I64, ValueType.I64, ValueType.I64),
                Collections.singletonList(ValueType.I64)
        ));
        setName("_hash");
    }

    @Override
    public long[] execute(long... parameters) {
        byte[] data = loadMemory((int) parameters[1], (int) parameters[2]);
        Algorithm a = Algorithm.values()[(int) parameters[0]];
        byte[] ret;
        switch (a) {
            case KECCAK256:
                ret = CryptoHelpers.keccak256(data);
                break;
            case SM3:
                ret = SM3Util.hash(data);
                break;
            default:
                throw new RuntimeException("unreachable");
        }
        if (parameters[4] != 0)
            putMemory((int) parameters[3], ret);
        return new long[]{ret.length};
    }

    enum Algorithm {
        SM3, KECCAK256
    }
}
