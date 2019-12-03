package org.tdf.sunflower.consensus.vrf;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.tdf.common.Block;
import org.tdf.common.Header;
import org.tdf.common.HexBytes;
import org.tdf.sunflower.consensus.poa.PoAUtils;
import org.tdf.sunflower.util.ByteUtil;

//@Getter
//@Setter
public class VrfGenesis {
    public HexBytes nonce;
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
    public Block getBlock(){
        Header h = Header.builder()
                .version(VrfConstants.BLOCK_VERSION)
                .hashPrev(VrfConstants.ZERO_BYTES)
                .merkleRoot(VrfConstants.ZERO_BYTES)
                .height(0)
                .createdAt(ByteUtil.byteArrayToLong(timestamp.getBytes()))
                .payload(VrfConstants.ZERO_BYTES).build();
        Block b = new Block(h);
        b.setHash(new HexBytes(PoAUtils.getHash(b)));
        return b;
    }
}
