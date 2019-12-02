package org.wisdom.consortium.vm.wasm.runtime.hosts;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.wisdom.consortium.vm.wasm.runtime.FunctionInstance;
import org.wisdom.consortium.vm.wasm.runtime.ModuleInstance;
import org.wisdom.consortium.vm.wasm.types.FunctionType;

import java.util.Objects;

public abstract class HostFunction implements FunctionInstance {
    private static final long HOST_FUNCTION_GAS = 10;

    @Getter(AccessLevel.PROTECTED)
    @Setter
    private ModuleInstance instance;

    @Setter(AccessLevel.PROTECTED)
    private FunctionType type;

    @Setter(AccessLevel.PROTECTED)
    private String name;

    public HostFunction() {
    }

    @Override
    public int parametersLength() {
        return getType().getParameterTypes().size();
    }

    @Override
    public int getArity() {
        return getType().getResultTypes().size();
    }

    @Override
    public FunctionType getType(){
        return type;
    };

    @Override
    public abstract long[] execute(long... parameters);

    @Override
    public boolean isHost() {
        return true;
    }

    public String getName(){
        return name;
    };

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HostFunction that = (HostFunction) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public long getGas() {
        return HOST_FUNCTION_GAS;
    }
}
