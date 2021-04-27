package org.tdf.sunflower.vm;

import org.tdf.common.types.Uint256;
import org.tdf.lotusvm.ModuleInstance;
import org.tdf.lotusvm.types.Module;
import org.tdf.sunflower.vm.abi.AbiDataType;

import java.nio.charset.StandardCharsets;

// webassembly blockchain interface
public abstract class WBI {
    public static void inject(Module m, ModuleInstance i, byte[] input) {

    }

    public static Object peek(ModuleInstance instance, int offset, AbiDataType type) {
        long startAndLen = instance.execute("__peek", offset, type.ordinal())[0];
        int start = (int) (startAndLen >>> 32);
        int len = (int) (startAndLen);
        byte[] bin = instance.getMemory().loadN(start, len);
        switch (type) {
            case STRING: {
                return new String(bin, StandardCharsets.UTF_8);
            }
            case U256: {
                return Uint256.of(bin);
            }
            case BYTES:
            case ADDRESS: {
                return bin;
            }
        }
        throw new RuntimeException("unexpected");
    }

    private static int mallocInternal(ModuleInstance instance, AbiDataType t, byte[] bin) {
        long ptr = instance.execute("__malloc", bin.length)[0];
        instance.getMemory().put((int) ptr, bin);
        long p = instance.execute("__change_t", t.ordinal(), ptr, bin.length)[0];
        int r = (int) p;
        if (r < 0)
            throw new RuntimeException("malloc failed: pointer is negative");
        return r;
    }

    public static int malloc(ModuleInstance instance, String s) {
        byte[] bin = s.getBytes(StandardCharsets.UTF_8);
        return mallocInternal(instance, AbiDataType.STRING, bin);
    }

    public static int malloc(ModuleInstance instance, Uint256 s) {
        byte[] bin = s.getNoLeadZeroesData();
        return mallocInternal(instance, AbiDataType.U256, bin);
    }

    public static int mallocBytes(ModuleInstance instance, byte[] bin) {
        return mallocInternal(instance, AbiDataType.BYTES, bin);
    }

    public static int mallocAddress(ModuleInstance instance, byte[] address) {
        return mallocInternal(instance, AbiDataType.ADDRESS, address);
    }
}
