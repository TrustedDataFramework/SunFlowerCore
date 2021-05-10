package org.tdf.sunflower.vm;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.tdf.lotusvm.ModuleInstance;
import org.tdf.sunflower.TestUtils;
import org.tdf.sunflower.vm.hosts.Hosts;


@RunWith(JUnit4.class)
public class RuntimeTest {
    private static final String WASM_FILE_PATH = "testdata/hello.wasm";

    // use environment FILE_PATH to locate your wasm file

    @Test
    public void testGetString() throws Exception {
// C:\Users\Sal\Documents\GitHub\sunflower\wasmer\as-example\build

    }

    @Test
    public void testDecimalFailedHostFunction() throws Exception {
        String filename = System.getenv("FILE_PATH");
        if (filename == null || "".equals(filename.trim())) return;
        ModuleInstance instance =
            ModuleInstance.builder()
                .binary(TestUtils.readClassPathFileAsByteArray(WASM_FILE_PATH))
                .hostFunctions(new Hosts().getAll())
                .build();
        Exception e = null;
        try {
            instance.execute("testException");
        } catch (Exception e2) {
            e = e2;
        }
        assert e != null;
    }
}
