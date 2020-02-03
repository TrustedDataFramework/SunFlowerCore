package org.tdf.sunflower.consensus.poa;

import org.tdf.common.serialize.Codec;
import org.tdf.common.store.ByteArrayMapStore;
import org.tdf.common.trie.Trie;
import org.tdf.common.util.HexBytes;
import org.tdf.crypto.HashFunctions;
import org.tdf.rlp.RLPCodec;
import org.tdf.sunflower.types.Block;
import org.tdf.sunflower.types.Header;
import org.tdf.sunflower.types.Transaction;

import java.util.List;

public class PoAUtils {
    public static byte[] getHash(Transaction transaction) {
        return HashFunctions.keccak256(RLPCodec.encode(transaction));
    }

    /**
     * get transactions root of block body, any modification of transaction or their order
     * will result in a totally different transactions root
     * light client cloud require a merkle proof to verify the existence of a transaction in the block {@link Trie#getProof(Object)}
     * @param transactions list of transaction in sequential
     * @return transactions root
     */
    public static byte[] getTransactionsRoot(List<Transaction> transactions) {
        Trie<Integer, Transaction> tmp = Trie.<Integer, Transaction>builder()
                .hashFunction(HashFunctions::keccak256)
                .keyCodec(Codec.newInstance(RLPCodec::encode, RLPCodec::decodeInt))
                .valueCodec(Codec.newInstance(RLPCodec::encode, x -> RLPCodec.decode(x, Transaction.class)))
                .store(new ByteArrayMapStore<>())
                .build();

        for (int i = 0; i < transactions.size(); i++) {
            tmp.put(i, transactions.get(i));
        }
        return tmp.commit();
    }

    public static byte[] getHash(Block block) {
        block.setTransactionsRoot(HexBytes.fromBytes(getTransactionsRoot(block.getBody())));
        return getHash(block.getHeader());
    }

    public static byte[] getHash(Header header) {
        return HashFunctions.keccak256(RLPCodec.encode(header));
    }
}
