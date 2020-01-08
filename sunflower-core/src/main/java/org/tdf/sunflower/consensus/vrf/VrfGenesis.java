package org.tdf.sunflower.consensus.vrf;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.tdf.sunflower.types.Block;
import org.tdf.sunflower.types.Header;
import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.consensus.poa.PoAUtils;
import org.tdf.sunflower.consensus.vrf.struct.VrfPrivateKey;
import org.tdf.sunflower.consensus.vrf.util.VrfConstants;
import org.tdf.sunflower.consensus.vrf.util.VrfUtil;
import org.tdf.sunflower.util.ByteUtil;

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

    public static class MinerInfo {
        @JsonProperty("addr")
        public String address;
    }

    public List<MinerInfo> miners;

    @JsonIgnore
    public Block getBlock(VrfConfig vrfConfig) throws IOException {
        long blockNum = ByteUtil.byteArrayToLong(number.getBytes());
        Header header = Header.builder().version(VrfConstants.BLOCK_VERSION).hashPrev(parentHash)
                .merkleRoot(VrfConstants.ZERO_BYTES).height(blockNum)
                .createdAt(ByteUtil.byteArrayToLong(timestamp.getBytes())).payload(VrfConstants.ZERO_BYTES).build();

        Block block = new Block(header);

        /*
         * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! Some issues here, there is a loop,
         * need to clarify: current block hash is used to generate ProposalProof, but
         * final block hash is generated partly from ProposalProof
         */
        setPayload(block, vrfConfig);

        block.setHash(HexBytes.fromBytes(PoAUtils.getHash(block)));

        return block;
    }

    private void setPayload(Block block, VrfConfig vrfConfig) throws IOException {
        long blockNum = block.getHeight();
        VrfPrivateKey vrfSk = VrfUtil.getVrfPrivateKeyDummy();
        byte[] vrfPk = vrfSk.generatePublicKey().getEncoded();
        int round = 0;
        byte[] payloadBytes = VrfUtil.genPayload(blockNum, round, seed, coinbase, difficulty,
                HexBytes.fromBytes(PoAUtils.getHash(block)), vrfSk, vrfPk, vrfConfig);
        HexBytes payload = HexBytes.fromBytes(payloadBytes);
        block.setPayload(payload);
    }
}
