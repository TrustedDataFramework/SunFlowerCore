package org.tdf.sunflower.consensus.vrf;

import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.consensus.poa.PoAUtils;
import org.tdf.sunflower.facade.HashPolicy;
import org.tdf.sunflower.types.Block;
import org.tdf.sunflower.types.Header;
import org.tdf.sunflower.types.Transaction;

public class VrfHashPolicy implements HashPolicy {
    static final HashPolicy HASH_POLICY = new VrfHashPolicy();

    @Override
    public HexBytes getHash(Block block) {
        return new HexBytes(PoAUtils.getHash(block));
    }

    @Override
    public HexBytes getHash(Transaction transaction) {
        return new HexBytes(PoAUtils.getHash(transaction));
    }

    @Override
    public HexBytes getHash(Header header) {
        return new HexBytes(PoAUtils.getHash(header));
    }
}
