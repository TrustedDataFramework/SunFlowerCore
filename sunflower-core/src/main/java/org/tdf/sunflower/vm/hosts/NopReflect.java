package org.tdf.sunflower.vm.hosts;

import org.tdf.sunflower.vm.abi.ContractCall;

public class NopReflect extends Reflect{

    public NopReflect() {
        super(null);
    }

    @Override
    public long[] execute(long... longs) {
        throw new UnsupportedOperationException();
    }
}
