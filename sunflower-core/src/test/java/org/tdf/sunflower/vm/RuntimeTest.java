package org.tdf.sunflower.vm;

import com.google.common.io.ByteStreams;
import com.google.common.primitives.Bytes;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.tdf.common.HexBytes;
import org.tdf.lotusvm.runtime.ModuleInstance;
import org.tdf.sunflower.TestUtils;
import org.tdf.sunflower.util.FileUtils;
import org.tdf.sunflower.vm.abi.Context;
import org.tdf.sunflower.vm.hosts.Hosts;

import java.nio.charset.StandardCharsets;
import java.util.*;

@RunWith(JUnit4.class)
public class RuntimeTest {
    private static final String WASM_FILE_PATH = "testdata/hello.wasm";

    // use environment FILE_PATH to locate your wasm file
    @Test
    public void testInvokeFile() throws Exception {

        byte[] data = Bytes.concat("hello world!!!!".getBytes(StandardCharsets.UTF_8), new byte[]{0});
        ModuleInstance instance = new ModuleInstance(
                ModuleInstance.Config.builder()
                        .binary(TestUtils.readClassPathFileAsByteArray(WASM_FILE_PATH))
                        .hostFunctions(new Hosts().withPayload(data).getAll())
                        .build()
        );
        instance.execute("invoke");
        System.out.println(instance.getGas());
    }


    @Test
    public void testGetString() throws Exception {
// C:\Users\Sal\Documents\GitHub\sunflower\wasmer\as-example\build
        String filename = System.getenv("FILE_PATH");
        if (filename == null || "".equals(filename.trim())) return;
        byte[] data = Bytes.concat("hello world!!!!".getBytes(StandardCharsets.UTF_8), new byte[]{0});
        Hosts hosts = new Hosts().withPayload(data);
        ModuleInstance instance = new ModuleInstance(
                ModuleInstance.Config.builder()
                        .binary(TestUtils.readClassPathFileAsByteArray(WASM_FILE_PATH))
                        .hostFunctions(hosts.getAll())
                        .build()
        );
        instance.execute("getString");
        System.out.println(new String(hosts.getResult(), StandardCharsets.UTF_8));
        System.out.println(instance.getGas());
    }

    @Test
    public void testContextHostFunction() throws Exception {
        Context context = Context.builder()
                .method("abcdf")
                .transactionHash(new HexBytes(new byte[]{0x00, 0x01}))
                .parentBlockHash(new HexBytes(new byte[]{0x00, 0x04}))
                .sender(new HexBytes(new byte[]{0x00, 0x02}))
                .recipient(new HexBytes(new byte[]{0x00, 0x03}))
                .amount(1)
                .gasPrice(2)
                .gasLimit(3)
                .blockTimestamp(4)
                .transactionTimestamp(5)
                .blockHeight(6)
                .build();
        ModuleInstance instance = new ModuleInstance(
                ModuleInstance.Config.builder()
                        .binary(ByteStreams.toByteArray(FileUtils.getResource(System.getenv("FILE_PATH")).getInputStream()))
                        .hostFunctions(new Hosts().withContext(context).getAll())
                        .build()
        );
        instance.execute("printContext");
    }

    @Test
    public void testJsonBuilderPutJson() throws Exception {
        String filename = System.getenv("FILE_PATH");
        if (filename == null || "".equals(filename.trim())) return;
        ModuleInstance instance = new ModuleInstance(
                ModuleInstance.Config.builder()
                        .binary(ByteStreams.toByteArray(FileUtils.getResource(System.getenv("FILE_PATH")).getInputStream()))
                        .hostFunctions(new Hosts().getAll())
                        .build());
        instance.execute("testJSON");

    }

    @Test
    public void testDecimalHostFunction() throws Exception {
        String filename = System.getenv("FILE_PATH");
        if (filename == null || "".equals(filename.trim())) return;
        ModuleInstance instance = new ModuleInstance(
                ModuleInstance.Config.builder()
                        .binary(ByteStreams.toByteArray(FileUtils.getResource(System.getenv("FILE_PATH")).getInputStream()))
                        .hostFunctions(new Hosts().getAll())
                        .build());
        instance.execute("addtest");
    }

    @Test
    public void testDecimalFailedHostFunction() throws Exception {
        String filename = System.getenv("FILE_PATH");
        if (filename == null || "".equals(filename.trim())) return;
        ModuleInstance instance = new ModuleInstance(
                ModuleInstance.Config.builder()
                        .binary(ByteStreams.toByteArray(FileUtils.getResource(System.getenv("FILE_PATH")).getInputStream()))
                        .hostFunctions(new Hosts().getAll())
                        .build());
        Exception e = null;
        try {
            instance.execute("testException");
        } catch (Exception e2) {
            e = e2;
        }
        assert e != null;
    }
}
