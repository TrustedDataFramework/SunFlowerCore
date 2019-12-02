package org.wisdom.consortium.vm.wasm.section;

public enum ImportType {
    // import a function
    TYPE_INDEX,
    TABLE_TYPE,
    // import a memory
    MEMORY_TYPE,
    GLOBAL_TYPE
}
