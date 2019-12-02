package org.wisdom.consortium.consensus.vrf;

import org.wisdom.common.Block;
import org.wisdom.common.HashPolicy;
import org.wisdom.common.Header;
import org.wisdom.common.HexBytes;
import org.wisdom.common.Transaction;
import org.wisdom.consortium.consensus.poa.PoAUtils;

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
