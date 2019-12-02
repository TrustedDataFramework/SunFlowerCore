package org.wisdom.consortium.vm.wasm.section;

import lombok.Getter;
import org.wisdom.consortium.vm.wasm.BytesReader;
import org.wisdom.consortium.vm.wasm.types.FunctionType;

import java.util.List;

/**
 * The type section has the id 1. It decodes into a vector of function types that represent the types component of a
 * module.
 */
public class TypeSection extends Section{
    public TypeSection(SectionID id, long size, BytesReader contents) {
        super(id, size, contents);
    }

    @Getter
    private List<FunctionType> functionTypes;

    @Override
    void readPayload() {
        BytesReader reader = getPayload();
        functionTypes = FunctionType.readFunctionTypesFrom(reader);
    }
}
