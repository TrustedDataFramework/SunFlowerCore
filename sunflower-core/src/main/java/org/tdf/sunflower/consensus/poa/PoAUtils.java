package org.tdf.sunflower.consensus.poa;

import com.google.common.hash.Hashing;
import com.google.common.primitives.Bytes;
import org.tdf.common.util.HexBytes;
import org.tdf.rlp.RLPCodec;
import org.tdf.sunflower.types.Block;
import org.tdf.sunflower.types.Header;
import org.tdf.sunflower.types.Transaction;

import java.util.List;

public class PoAUtils {
    public static byte[] getHash(Transaction transaction) {
        return Hashing.sha256().hashBytes(RLPCodec.encode(transaction)).asBytes();
    }

    public static byte[] merkleHash(List<Transaction> transactions) {
        byte[] all = transactions.stream().map(PoAUtils::getHash)
                .reduce(new byte[0], Bytes::concat);
        return Hashing.sha256().hashBytes(all).asBytes();
    }

    public static byte[] getHash(Block block) {
        block.setMerkleRoot(new HexBytes(merkleHash(block.getBody())));
        return Hashing.sha256().hashBytes(RLPCodec.encode(block.getHeader())).asBytes();
    }

    public static byte[] getHash(Header header){
        return Hashing.sha256().hashBytes(RLPCodec.encode(header)).asBytes();
    }
}
