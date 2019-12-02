package org.wisdom.consortium.vm.wasm.runtime;

import org.wisdom.consortium.vm.wasm.types.FunctionType;

public interface FunctionInstance {
    int parametersLength();
    int getArity();
    FunctionType getType();
    long[] execute(long... parameters);
    long getGas();
    boolean isHost();
}
