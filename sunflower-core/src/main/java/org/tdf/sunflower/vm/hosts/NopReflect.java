package org.tdf.sunflower.vm.hosts;

public class NopReflect extends Reflect{

    public NopReflect() {
        super(null);
    }

    @Override
    public long[] execute(long... longs) {
        throw new UnsupportedOperationException();
    }
}
