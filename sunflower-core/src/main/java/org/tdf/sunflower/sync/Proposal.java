package org.tdf.sunflower.sync;

import org.tdf.sunflower.types.Block;

public class Proposal {
    private Block block;

    public Proposal(Block block) {
        this.block = block;
    }

    public Proposal() {
    }

    public Block getBlock() {
        return this.block;
    }

    public void setBlock(Block block) {
        this.block = block;
    }
}
