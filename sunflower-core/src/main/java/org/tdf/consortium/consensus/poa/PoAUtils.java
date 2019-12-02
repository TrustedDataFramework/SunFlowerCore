package org.tdf.consortium.consensus.poa;

import com.google.common.hash.Hashing;
import com.google.common.primitives.Bytes;
import org.tdf.common.Block;
import org.tdf.common.Header;
import org.tdf.common.HexBytes;
import org.tdf.common.Transaction;
import org.tdf.util.CommonUtil;

import java.util.List;

public class PoAUtils {
    public static byte[] getHash(Transaction transaction) {
        return Hashing.sha256().hashBytes(CommonUtil.getRaw(transaction)).asBytes();
    }

    public static byte[] merkleHash(List<Transaction> transactions) {
        byte[] all = transactions.stream().map(PoAUtils::getHash)
                .reduce(new byte[0], Bytes::concat);
        return Hashing.sha256().hashBytes(all).asBytes();
    }

    public static byte[] getHash(Block block) {
        block.setMerkleRoot(new HexBytes(merkleHash(block.getBody())));
        return Hashing.sha256().hashBytes(CommonUtil.getRaw(block.getHeader())).asBytes();
    }

    public static byte[] getHash(Header header){
        return Hashing.sha256().hashBytes(CommonUtil.getRaw(header)).asBytes();
    }
}
