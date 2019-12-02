package org.wisdom.consortium.vm.wasm.section;

import lombok.Getter;
import org.wisdom.consortium.vm.wasm.BytesReader;
import org.wisdom.consortium.vm.wasm.Vector;

/**
 * Custom sections have the id 0. They are intended to be used for debugging information or third-party extensions,
 * and are ignored by the WebAssembly semantics. Their contents consist of a name further identifying the custom
 * section, followed by an uninterpreted sequence of bytes for custom use
 */
public class CustomSection extends Section{
    @Getter
    private String name;
    @Getter
    private byte[] data;

    public CustomSection(SectionID id, long size, BytesReader contents) {
        super(id, size, contents);
    }

    @Override
    void readPayload() {
        BytesReader reader = getPayload();
        name = Vector.readStringFrom(reader);
        data = reader.readAll();
    }
}
