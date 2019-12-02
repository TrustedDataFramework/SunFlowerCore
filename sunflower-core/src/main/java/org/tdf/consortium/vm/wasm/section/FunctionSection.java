package org.wisdom.consortium.vm.wasm.section;

import lombok.Getter;
import org.wisdom.consortium.vm.wasm.BytesReader;
import org.wisdom.consortium.vm.wasm.Vector;

/**
 * The function section has the id 3.
 * It decodes into a vector of type indices that represent the type ﬁelds of the functions in the funcs component of a module.
 * The locals and body ﬁelds of the respective functions are encoded separately in the code section.
 */
public class FunctionSection extends Section {
    public FunctionSection(SectionID id, long size, BytesReader payload) {
        super(id, size, payload);
    }

    @Getter
    private int[] typeIndices;

    @Override
    void readPayload() {
        BytesReader reader = getPayload();
        typeIndices = Vector.readUint32VectorFrom(reader);
    }
}
