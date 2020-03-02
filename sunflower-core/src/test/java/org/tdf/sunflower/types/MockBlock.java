package org.tdf.sunflower.types;

import lombok.NonNull;
import org.tdf.common.util.HexBytes;

public class MockBlock extends Block{
    private HexBytes hash;

    @Override
    public HexBytes getHash() {
        return hash;
    }

    public void setHash(HexBytes hash) {
        this.hash = hash;
    }

    public MockBlock(@NonNull Header header) {
        super(header);
    }
}
