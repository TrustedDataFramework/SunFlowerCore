package org.tdf.sunflower.consensus.vrf;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.tdf.common.serialize.Codec;
import org.tdf.common.store.ByteArrayMapStore;
import org.tdf.common.trie.Trie;
import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.consensus.vrf.struct.VrfPrivateKey;
import org.tdf.sunflower.consensus.vrf.util.VrfConstants;
import org.tdf.sunflower.consensus.vrf.util.VrfUtil;
import org.tdf.sunflower.types.Block;
import org.tdf.sunflower.types.CryptoContext;
import org.tdf.sunflower.types.Header;
import org.tdf.sunflower.types.Transaction;
import org.tdf.sunflower.util.ByteUtil;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

//@Getter
//@Setter
public class VrfGenesis {
    public HexBytes seed;
    public HexBytes timestamp;
    public HexBytes extraData;
    public HexBytes gasLimit;
    public HexBytes difficulty;
    public HexBytes coinbase;
    public HexBytes number;
    public HexBytes gasUsed;
    public HexBytes parentHash;
    public HexBytes hashBlock;
    public List<MinerInfo> miners;
    public Map<String, Long> alloc;

    @JsonIgnore
    public Block getBlock(VrfConfig vrfConfig) throws IOException {
        long blockNum = ByteUtil.byteArrayToLong(number.getBytes());

        Trie<?, ?> trie = Trie.<byte[], byte[]>builder().keyCodec(Codec.identity()).valueCodec(Codec.identity())
                .store(new ByteArrayMapStore<>()).hashFunction(CryptoContext::hash).build();

        HexBytes emptyRoot = Transaction.calcTxTrie(Collections.emptyList());

        Header header = null;
//                Header.builder().version(VrfConstants.BLOCK_VERSION).hashPrev(parentHash)
//                .transactionsRoot(emptyRoot).height(blockNum).createdAt(ByteUtil.byteArrayToLong(timestamp.getBytes()))
//                .payload(VrfConstants.ZERO_BYTES).stateRoot(trie.getNullHash()).build();

        Block block = new Block(header);
        /*
         * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! Some issues here, there is a loop,
         * need to clarify: current block hash is used to generate ProposalProof, but
         * final block hash is generated partly from ProposalProof
         */
        setPayload(block, vrfConfig);

        return block;
    }

    private void setPayload(Block block, VrfConfig vrfConfig) throws IOException {
        long blockNum = block.getHeight();
        VrfPrivateKey vrfSk = VrfUtil.getVrfPrivateKeyDummy();
        byte[] vrfPk = vrfSk.generatePublicKey().getEncoded();
        int round = 0;
        byte[] payloadBytes = VrfUtil.genPayload(blockNum, round, seed, coinbase, difficulty, block.getHash(), vrfSk,
                vrfPk, vrfConfig);
        HexBytes payload = HexBytes.fromBytes(payloadBytes);
//        block.setPayload(payload);
    }

    public static class MinerInfo {
        @JsonProperty("addr")
        public HexBytes address;
        @JsonProperty("collateral")
        public long collateral;
    }
}
