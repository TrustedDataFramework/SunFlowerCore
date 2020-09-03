package org.tdf.sunflower.vm.hosts;

public class UnsupportedTransfer extends Transfer {
    public UnsupportedTransfer() {
        super(null, null);
    }

    @Override
    public long[] execute(long... parameters) {
        throw new UnsupportedOperationException();
    }
}
