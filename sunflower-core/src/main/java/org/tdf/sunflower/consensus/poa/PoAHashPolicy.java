package org.tdf.sunflower.consensus.poa;


import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.facade.HashPolicy;
import org.tdf.sunflower.types.Block;
import org.tdf.sunflower.types.Header;
import org.tdf.sunflower.types.Transaction;

public class PoAHashPolicy implements HashPolicy {
    static final HashPolicy HASH_POLICY = new PoAHashPolicy();

    @Override
    public HexBytes getHash(Block block) {
        return HexBytes.fromBytes(PoAUtils.getHash(block));
    }

    @Override
    public HexBytes getHash(Transaction transaction) {
        return HexBytes.fromBytes(PoAUtils.getHash(transaction));
    }

    @Override
    public HexBytes getHash(Header header) {
        return HexBytes.fromBytes(PoAUtils.getHash(header));
    }
}
