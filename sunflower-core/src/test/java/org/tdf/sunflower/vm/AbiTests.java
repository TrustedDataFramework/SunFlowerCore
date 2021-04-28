package org.tdf.sunflower.vm;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.tdf.common.types.ByteUtil;
import org.tdf.common.util.HexBytes;
import org.tdf.crypto.CryptoHelpers;
import org.tdf.lotusvm.ModuleInstance;
import org.tdf.lotusvm.types.CustomSection;
import org.tdf.lotusvm.types.Module;
import org.tdf.rlp.RLPElement;
import org.tdf.rlp.RLPList;
import org.tdf.sunflower.vm.abi.Abi;
import org.tdf.sunflower.vm.hosts.Log;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RunWith(JUnit4.class)
public class AbiTests {

    @Test
    public void test0() throws Exception{
        byte[] bytes = Files.readAllBytes(Paths.get("/Users/sal/Documents/Github/SunFlowerCore/sdk-v2/bin"));
        RLPList li = RLPElement.fromEncoded(bytes).asRLPList();
        byte[] code = li.get(0).asBytes();
        byte[] input = li.get(1).asBytes();
        Log log = new Log();

        Module m = new Module(code);
        ModuleInstance ins = ModuleInstance.builder()
                .module(m)
                .hostFunctions(Collections.singleton(log))
                .build();

        CustomSection sec =
                m.getCustomSections().stream().filter(x -> x.getName().equals("abi")).findAny().get();

        // 1. restore abi
        String abiJson = new String(sec.getData());
        Abi abi = Abi.fromJson(abiJson);

        // 2. find function or constructor by selector
        String selector = HexBytes.encode(Arrays.copyOfRange(input, 0, 4));

        System.out.println(selector);

        Abi.Constructor constructor = abi.findConstructor();


    }
}
