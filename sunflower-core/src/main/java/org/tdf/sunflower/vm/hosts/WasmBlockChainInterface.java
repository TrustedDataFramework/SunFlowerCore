package org.tdf.sunflower.vm.hosts;

import org.tdf.common.types.Uint256;
import org.tdf.lotusvm.ModuleInstance;
import org.tdf.sunflower.vm.abi.AbiDataType;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public abstract class WasmBlockChainInterface {

    // 元数据，包含了字符串编码等信息
    public static byte[] getMeta(ModuleInstance instance) {
        int offset = (int) instance.execute("__meta")[0];
        byte metaLen =instance.getMemory().getData()[offset];
        return instance.getMemory().loadN(offset, metaLen);
    }

    public static Charset getCharSet(ModuleInstance instance) {
        byte c = getMeta(instance)[1];
        if (c == 0)
            return StandardCharsets.UTF_8;
        if (c == 1)
            return StandardCharsets.UTF_16;
        if(c == 2)
            return StandardCharsets.UTF_16LE;
        throw new RuntimeException("get charset failed unknown charset");
    }

    public static Object mpeek(ModuleInstance instance, int offset, AbiDataType type){
        long lenAndStart = instance.execute("__mpeek", offset, type.ordinal())[0];
        int len = (int) (lenAndStart >>> 32);
        byte[] bin = instance.getMemory().loadN((int) lenAndStart, len);
        switch (type){
            case STRING:{
                return new String(bin, getCharSet(instance));
            }
            case U256:{
                return Uint256.of(bin);
            }
            case BYTES:
            case ADDRESS:{
                return bin;
            }
        }
        throw new RuntimeException("unexpected");
    }

    public static int malloc(ModuleInstance instance, String s) {
        byte[] bin = s.getBytes(getCharSet(instance));
        long ptr = instance.execute("__malloc", bin.length, AbiDataType.STRING.ordinal())[0];
        instance.getMemory().put((int) ptr, bin);
        return (int) (ptr >>> 32);
    }

    public static int malloc(ModuleInstance instance, Uint256 s) {
        byte[] bin = s.getNoLeadZeroesData();
        long ptr = instance.execute("__malloc", bin.length, AbiDataType.U256.ordinal())[0];
        instance.getMemory().put((int) ptr, bin);
        return (int) (ptr >>> 32);
    }

    public static int mallocBytes(ModuleInstance instance, byte[] bin) {
        long ptr = instance.execute("__malloc", bin.length, AbiDataType.BYTES.ordinal())[0];
        instance.getMemory().put((int) ptr, bin);
        return (int) (ptr >>> 32);
    }

    public static int mallocAddress(ModuleInstance instance, byte[] address) {
        long ptr = instance.execute("__malloc", address.length, AbiDataType.ADDRESS.ordinal())[0];
        instance.getMemory().put((int) ptr, address);
        return (int) (ptr >>> 32);
    }
}
