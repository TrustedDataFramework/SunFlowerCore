package org.wisdom.consortium.vm.wasm;

import com.google.common.io.ByteStreams;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.wisdom.consortium.util.FileUtils;
import org.wisdom.consortium.vm.wasm.section.Module;

import java.io.File;


@RunWith(JUnit4.class)
public class ModuleTest {

    @Test
    public void test1() throws Exception {
//        Module module = new Module(ByteStreams.toByteArray(FileUtils.getResource("testdata/spec/br.wasm").getInputStream()));

        File file = FileUtils.getResource("testdata/spec")
                .getFile();
        File[] dirFile = file.listFiles();
        //遍历数组
        for (File f : dirFile) {
            if(f.getName().equals("address.wasm")) continue;
            if (f.isFile() && f.getName().endsWith(".wasm")) {
                //如果是文件夹就递归
                Module module = new Module(ByteStreams.toByteArray(FileUtils.getResource(f.getAbsolutePath()).getInputStream()));
            }
        }
    }

    private BytesReader getReader() {
        return new BytesReader(new byte[]{0x00, 0x01, 0x02, 0x03});
    }

    @Test
    public void testBytesReaderPeek() {
        BytesReader reader = getReader();
        assert reader.peek() == reader.read();
        assert reader.peek() == reader.read();
        assert reader.peek() == reader.read();
        assert reader.peek() == reader.read();
    }

    @Test
    public void testReadSome() throws Exception {
        BytesReader reader = getReader();
        byte[] data = reader.read(2);
        assert data[0] == 0x00;
        assert data[1] == 0x01;
        data = reader.read(2);
        assert data[0] == 0x02;
        assert data[1] == 0x03;
    }

    @Test
    public void testReadAll() throws Exception {
        BytesReader reader = getReader();
        reader.read(2);
        byte[] data = reader.readAll();
        assert data[0] == 0x02;
        assert data[1] == 0x03;
        assert reader.remaining() == 0;
    }

    @Test
    public void testAddWasm() throws Exception {
        Module m = new Module(ByteStreams.toByteArray(FileUtils.getResource("expression-tests/add.wasm").getInputStream()));
        assert m.getFunctionSection() != null;
    }

    @Test
    public void testExportAdd() throws Exception {
        Module m = new Module(ByteStreams.toByteArray(FileUtils.getResource("expression-tests/export-add.wasm").getInputStream()));
        assert m.getFunctionSection() != null;
        assert m.getExportSection() != null;
    }

    @Test
    public void testCall() throws Exception {
        Module m = new Module(ByteStreams.toByteArray(FileUtils.getResource("expression-tests/call-another.wasm").getInputStream()));
        assert m.getFunctionSection() != null;
        assert m.getExportSection() != null;
    }

    @Test
    public void testAddress() throws Exception {
        Module m = new Module(ByteStreams.toByteArray(FileUtils.getResource("testdata/spec/address.wasm").getInputStream()));
        assert m.getFunctionSection() != null;
        assert m.getExportSection() != null;
    }

    @Test
    public void testMemory() throws Exception{
        Module m = new Module(ByteStreams.toByteArray(FileUtils.getResource("expression-tests/memory.wasm").getInputStream()));
        assert m.getDataSection() != null;
    }

    @Test
    public void testTable() throws Exception{
        Module m = new Module(ByteStreams.toByteArray(FileUtils.getResource("expression-tests/table.wasm").getInputStream()));
        assert m.getTableSection() != null;
    }
}
