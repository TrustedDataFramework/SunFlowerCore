package org.tdf.sunflower.consensus.poa;

import org.tdf.common.util.HashUtil;
import org.tdf.rlpstream.Rlp;
import org.tdf.sunflower.types.Header;

public class PoaUtils {

    public static byte[] getRawHash(Header header) {
        byte[] encoded = Rlp.encode(new Object[] {
            header.getHashPrev(), header.getUnclesHash(), header.getCoinbase(), header.getStateRoot(),
            header.getTransactionsRoot(), header.getReceiptTrieRoot(), header.getLogsBloom(), header.getDifficulty(),
            header.getHeight(), header.getGasLimit(), header.getGasUsed(), header.getCreatedAt(),
            header.getMixHash(), header.getNonce()
        });
        return HashUtil.sha3(encoded);
    }
}
