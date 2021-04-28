package org.tdf.sunflower.vm;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.tdf.crypto.CryptoHelpers;
import org.tdf.lotusvm.ModuleInstance;
import org.tdf.lotusvm.types.CustomSection;
import org.tdf.lotusvm.types.Module;
import org.tdf.sunflower.types.CryptoContext;
import org.tdf.sunflower.vm.abi.Abi;
import org.tdf.sunflower.vm.abi.WbiType;
import org.tdf.sunflower.vm.hosts.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Collectors;

@RunWith(JUnit4.class)
public class AbiTests {

    @Test
    public void test0() throws Exception{
        CryptoContext.keccak256 = CryptoHelpers::keccak256;

        byte[] code = Files.readAllBytes(Paths.get("/Users/sal/Documents/Github/SunFlowerCore/sdk-v2/bin"));

        Log log = new Log();

        Module m = new Module(code);
        ModuleInstance ins = ModuleInstance.builder()
                .module(m)
                .hostFunctions(Collections.singleton(log))
                .build();

        CustomSection sec =
                m.getCustomSections().stream().filter(x -> x.getName().equals("__abi")).findAny().get();

        Optional<CustomSection> init =
                m.getCustomSections().stream().filter(x -> x.getName().equals("__init")).findAny();

        // 1. restore abi
        String abiJson = new String(sec.getData());
        Abi abi = Abi.fromJson(abiJson);

        // 2. find function or constructor by selector
        if (init.isPresent()) {
            byte[] input = init.get().getData();

            Abi.Constructor constructor = abi.findConstructor();
            if (constructor == null)
                return;
            String joined = constructor.inputs.stream().map(x -> x.type.getName()).collect(Collectors.joining(","));

            // malloc t,t,t into wasm runtime
            long paramsPtr = WBI.malloc(ins, joined);
            long inputPtr = WBI.mallocBytes(ins, input);

            // call __abi_decode
            byte[] decoded = (byte[]) WBI.peek(ins, (int) ins.execute("__abi_decode", paramsPtr, inputPtr)[0], WbiType.BYTES);

            long[] args = new long[constructor.inputs.size()];

            for(int i = 0; i < args.length; i++) {
                args[i] = ByteBuffer.wrap(decoded, i * 8,  8).order(ByteOrder.BIG_ENDIAN).getLong();
            }

            ins.execute("init", args);
        }


    }
}
