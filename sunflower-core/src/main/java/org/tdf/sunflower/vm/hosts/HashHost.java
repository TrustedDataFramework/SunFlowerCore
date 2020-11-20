package org.tdf.sunflower.vm.hosts;

import org.tdf.crypto.CryptoHelpers;
import org.tdf.gmhelper.SM3Util;
import org.tdf.lotusvm.runtime.HostFunction;
import org.tdf.lotusvm.types.FunctionType;
import org.tdf.lotusvm.types.ValueType;
import org.tdf.sunflower.vm.abi.AbiDataType;

import java.util.Arrays;
import java.util.Collections;

public class HashHost extends HostFunction {
    public HashHost() {
        setType(new FunctionType(
                // offset, length, offset
                Arrays.asList(ValueType.I64, ValueType.I64),
                Collections.singletonList(ValueType.I64)
        ));
        setName("_hash");
    }

    @Override
    public long[] execute(long... parameters) {
        byte[] data = getData(parameters);
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
        long r = WasmBlockChainInterface.mallocBytes(getInstance(), ret);
        return new long[]{r};
    }

    byte[] getData(long... parameters){
        return (byte[]) WasmBlockChainInterface
        .mpeek(getInstance(), (int) parameters[1], AbiDataType.BYTES);
    }

    enum Algorithm {
        SM3, KECCAK256
    }
}
