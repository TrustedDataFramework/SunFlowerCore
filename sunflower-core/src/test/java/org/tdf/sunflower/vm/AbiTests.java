package org.tdf.sunflower.vm;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.tdf.common.util.HexBytes;
import org.tdf.lotusvm.types.CustomSection;
import org.tdf.lotusvm.types.Module;
import org.tdf.rlp.RLPCodec;
import org.tdf.rlp.RLPElement;
import org.tdf.rlp.RLPList;
import org.tdf.sunflower.vm.abi.Abi;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

@RunWith(JUnit4.class)
public class AbiTests {

    @Test
    public void test0() throws Exception{
        byte[] bytes = Files.readAllBytes(Paths.get("/Users/sal/Documents/Github/SunFlowerCore/sdk-v2/bin"));
        RLPList li = RLPElement.fromEncoded(bytes).asRLPList();
        byte[] code = li.get(0).asBytes();
        byte[] input = li.get(1).asBytes();

        Module m = new Module(code);
        CustomSection sec =
                m.getCustomSections().stream().filter(x -> x.getName().equals("abi")).findAny().get();

        // 1. restore abi
        String abiJson = new String(sec.getData());
        Abi abi = Abi.fromJson(abiJson);

        // 2. find function or constructor by selector
        String selector = HexBytes.encode(Arrays.copyOfRange(input, 0, 4));

        System.out.println(selector);

        Abi.Constructor constructor = abi.findConstructor();
        List<?> decoded = constructor.decode(input);
        System.out.println("");
    }
}
