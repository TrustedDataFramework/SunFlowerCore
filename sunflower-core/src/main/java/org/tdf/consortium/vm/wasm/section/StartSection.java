package org.wisdom.consortium.vm.wasm.section;

import lombok.Getter;
import org.wisdom.consortium.vm.wasm.BytesReader;

/**
 * The start section has the id 8. It decodes into an optional start function that represents the start component of a module.
 */
public class StartSection extends Section{
    public StartSection(SectionID id, long size, BytesReader payload) {
        super(id, size, payload);
    }

    @Getter
    private int functionIndex;

    @Override
    void readPayload() {
        functionIndex = getPayload().readVarUint32();
    }
}
