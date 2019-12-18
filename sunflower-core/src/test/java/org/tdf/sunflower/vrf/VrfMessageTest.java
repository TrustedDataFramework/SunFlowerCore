package org.tdf.sunflower.vrf;

import org.junit.Test;
import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.types.Block;
import org.tdf.sunflower.util.ByteUtil;

import java.io.IOException;

public class VrfMessageTest {

    public VrfMessageTest() {

    }

    @Test
    public void testBlock() throws IOException {
        Block block = new Block();
        long blockNum = 1234;
        String payloadStr = "12abcd";
        String blockHashStr = "abcdef";
        block.setPayload(new HexBytes(ByteUtil.hexStringToBytes(payloadStr)));
        block.setHashPrev(new HexBytes(ByteUtil.hexStringToBytes(blockHashStr)));
        block.setHeight(blockNum);

//        byte[] encode = Start.MAPPER.writeValueAsBytes(block);
//        Block blockDecoded = Start.MAPPER.readValue(encode, Block.class);
//        assert(blockDecoded.getHeight() == blockNum);
//        assert(ByteUtil.toHexString(blockDecoded.getHashPrev().getBytes()).equals(blockHashStr));
//        assert(ByteUtil.toHexString(blockDecoded.getPayload().getBytes()).equals(payloadStr));
    }
}
