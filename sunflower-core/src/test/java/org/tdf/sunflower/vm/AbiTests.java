package org.tdf.sunflower.vm;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.tdf.common.util.HexBytes;
import org.tdf.crypto.CryptoHelpers;
import org.tdf.lotusvm.ModuleInstance;
import org.tdf.lotusvm.runtime.HostFunction;
import org.tdf.lotusvm.types.CustomSection;
import org.tdf.lotusvm.types.Module;
import org.tdf.sunflower.types.CryptoContext;
import org.tdf.sunflower.vm.abi.Abi;
import org.tdf.sunflower.vm.abi.WbiType;
import org.tdf.sunflower.vm.hosts.Log;
import org.tdf.sunflower.vm.hosts.U256Host;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@RunWith(JUnit4.class)
public class AbiTests {

    @Test
    public void test0() throws Exception{
        CryptoContext.keccak256 = CryptoHelpers::keccak256;

        byte[] code = Files.readAllBytes(Paths.get("/Users/sal/Documents/Github/SunFlowerCore/sdk-v2/bin"));

        Set<HostFunction> hosts = new HashSet<>();
        hosts.add(new Log());
        hosts.add(new U256Host());

        Module m = new Module(code);
        ModuleInstance ins = ModuleInstance.builder()
                .module(m)
                .hostFunctions(hosts)
                .build();

        WBI.InjectResult r = WBI.inject(true, m, ins, null);
        if(r.isExecute()) {
            ins.execute(r.getFunction(), r.getPointers());
        }

        String input = "64561ec300000000000000000000000000000000000000000000000000000000000000a000000000000000000000000012691d3b267130bb57e622f880c6928d4ed9fac70000000000000000000000000000000000000000000000008ac7230489e80000000000000000000000000000000000000000000000000000000000000003944700000000000000000000000000000000000000000000000000000000000000e00000000000000000000000000000000000000000000000000000000000000004736f6d6500000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000001412691d3b267130bb57e622f880c6928d4ed9fac7000000000000000000000000";
        r = WBI.inject(false, m, ins, HexBytes.decode(input));
        if(r.isExecute()) {
            ins.execute(r.getFunction(), r.getPointers());
        }
    }
}
