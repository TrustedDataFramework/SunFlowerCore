package org.wisdom.consortium.vm.wasm.section;

import lombok.Getter;
import org.wisdom.consortium.vm.wasm.BytesReader;
import org.wisdom.consortium.vm.wasm.types.LimitType;

import java.util.List;

/**
 * The memory section has the id 5. It decodes into a vector of memories that represent the mems component of a module.
 */
public class MemorySection extends Section {

    @Getter
    private List<LimitType> memories;

    public MemorySection(SectionID id, long size, BytesReader payload) {
        super(id, size, payload);
    }

    @Override
    void readPayload() {
        BytesReader reader = getPayload();
        memories = LimitType.readLimitTypesFrom(reader);
    }
}
