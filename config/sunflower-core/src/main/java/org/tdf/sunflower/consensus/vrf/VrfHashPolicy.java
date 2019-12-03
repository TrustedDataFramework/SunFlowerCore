package org.tdf.sunflower.consensus.vrf;

import org.tdf.common.*;
import org.tdf.sunflower.consensus.poa.PoAUtils;

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
