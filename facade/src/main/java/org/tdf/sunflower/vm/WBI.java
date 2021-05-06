package org.tdf.sunflower.vm;

import lombok.Value;
import org.tdf.common.types.Uint256;
import org.tdf.common.util.BigIntegers;
import org.tdf.common.util.FastByteComparisons;
import org.tdf.common.util.HexBytes;
import org.tdf.lotusvm.ModuleInstance;
import org.tdf.lotusvm.types.CustomSection;
import org.tdf.lotusvm.types.Module;
import org.tdf.sunflower.vm.abi.Abi;
import org.tdf.sunflower.vm.abi.WbiType;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public abstract class WBI {

    @Value
    public static class InjectResult {
        String function;
        long[] pointers;
        boolean executable;
    }

    public static byte[] dropInit(byte[] code) {
        Module m = new Module(code);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        // drop __init sections
        int now = 0;
        for (CustomSection section : m.getCustomSections()) {
            if (section.getName().equals("__init")) {
                out.write(code, now, section.getOffset() - now);
                now = section.getLimit();
            }
        }
        out.write(code, now, code.length - now);
        return out.toByteArray();
    }

    public static byte[] extractInitData(Module m) {
        return m.getCustomSections().stream().filter(x -> x.getName().equals("__init")).findFirst()
            .map(CustomSection::getData).orElse(new byte[0]);
    }

    // the __init section is dropped before inject
    public static InjectResult inject(boolean create, Abi abi, ModuleInstance i, HexBytes input) {
        List<Abi.Entry.Param> params = null;
        HexBytes encoded = null;
        String function = null;

        // 2. for contract create, constructor is not necessarily
        if (create && abi.findConstructor() != null) {
            function = "init";
            params = abi.findConstructor().inputs;
            encoded = input;
        }

        // 3. for contract call, find function by signature
        if (!create) {
            Abi.Function f = abi.findFunction(x -> FastByteComparisons.equal(x.encodeSignature(), Arrays.copyOfRange(input.getBytes(), 0, 4)));
            Objects.requireNonNull(f);
            function = f.name;
            params = f.inputs;
            Objects.requireNonNull(params);
            encoded = input.slice(4);
        }

        if (params == null)
            return new InjectResult(function, new long[0], false);

        // malloc param types
        List<?> inputs = Abi.Entry.Param.decodeList(params, encoded.getBytes());
        long[] ret = new long[params.size()];

        for (int j = 0; j < inputs.size(); j++) {
            Abi.Entry.Param p = params.get(j);

            switch (p.type.getName()) {
                case "uint8":
                case "uint16":
                case "uint32":
                case "uint64": {
                    ret[j] = ((BigInteger) inputs.get(j)).longValueExact();
                    break;
                }
                case "uint":
                case "uint256": {
                    BigInteger b = (BigInteger) inputs.get(j);
                    ret[j] = WBI.malloc(i, Uint256.of(b));
                    break;
                }
                case "string": {
                    String s = (String) inputs.get(j);
                    ret[j] = WBI.malloc(i, s);
                    break;
                }
                case "address": {
                    byte[] addr = (byte[]) inputs.get(j);
                    ret[j] = WBI.mallocAddress(i, HexBytes.fromBytes(addr));
                    break;
                }
                default: {
                    if (p.type.getName().endsWith("]") || p.type.getName().endsWith(")")) {
                        throw new RuntimeException("array or tuple is not supported");
                    }

                    if (p.type.getName().startsWith("bytes")) {
                        byte[] data = (byte[]) inputs.get(j);
                        ret[j] = WBI.mallocBytes(i, HexBytes.fromBytes(data));
                    }
                    break;
                }
            }
        }
        return new InjectResult(function, ret, true);
    }

    // String / U256 / HexBytes
    public static Object peek(ModuleInstance instance, int offset, int type) {
        long t = Integer.toUnsignedLong(type);
        long startAndLen = instance.execute("__peek", offset, t)[0];
        int start = (int) (startAndLen >>> 32);
        int len = (int) (startAndLen);
        byte[] bin = instance.getMemory().loadN(start, len);
        switch (type) {
            case WbiType.STRING: {
                return new String(bin, StandardCharsets.UTF_8);
            }
            case WbiType.UINT_256: {
                return Uint256.of(bin);
            }
            case WbiType.BYTES:
            case WbiType.ADDRESS: {
                return HexBytes.fromBytes(bin);
            }
        }
        throw new RuntimeException("unexpected");
    }

    private static int mallocInternal(ModuleInstance instance, int type, byte[] bin) {
        long t = Integer.toUnsignedLong(type);
        long ptr = instance.execute("__malloc", bin.length)[0];
        instance.getMemory().put((int) ptr, bin);
        long p = instance.execute("__change_t", t, ptr, bin.length)[0];
        int r = (int) p;
        if (r < 0)
            throw new RuntimeException("malloc failed: pointer is negative");
        return r;
    }


    public static int malloc(ModuleInstance instance, String s) {
        byte[] bin = s.getBytes(StandardCharsets.UTF_8);
        return mallocInternal(instance, WbiType.STRING, bin);
    }

    public static int malloc(ModuleInstance instance, Uint256 s) {
        byte[] bin = s.getNoLeadZeroesData();
        return mallocInternal(instance, WbiType.UINT_256, bin);
    }

    public static int mallocBytes(ModuleInstance instance, HexBytes bin) {
        return mallocInternal(instance, WbiType.BYTES, bin.getBytes());
    }

    public static int mallocAddress(ModuleInstance instance, HexBytes address) {
        return mallocInternal(instance, WbiType.ADDRESS, address.getBytes());
    }
}
