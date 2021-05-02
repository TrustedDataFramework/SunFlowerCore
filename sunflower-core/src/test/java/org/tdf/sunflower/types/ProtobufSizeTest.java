package org.tdf.sunflower.types;

import com.google.protobuf.ByteString;
import com.google.protobuf.Timestamp;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.tdf.common.util.ByteUtil;
import org.tdf.rlp.RLPItem;
import org.tdf.rlp.RLPList;
import org.tdf.sunflower.consensus.pow.PoWBios;
import org.tdf.sunflower.proto.Code;
import org.tdf.sunflower.proto.Message;

@RunWith(JUnit4.class)
public class ProtobufSizeTest {

    @Test
    public void test0() {
        Message msg = Message.newBuilder()
                .setCode(Code.PING)
                .setCreatedAt(Timestamp.newBuilder().setSeconds(System.currentTimeMillis() / 1000))
                .setRemotePeer("http://localhost:8080")
                .setTtl(16)
                .setNonce(1000000)
                .setSignature(ByteString.copyFrom("hello world".getBytes()))
                .setBody(ByteString.copyFrom("hello world".getBytes()))
                .build();

        // 65
        System.out.println(msg.getSerializedSize());

        int sizeofRLP = RLPList.of(
                RLPItem.fromInt(Code.PING_VALUE),
                RLPItem.fromLong(msg.getCreatedAt().getSeconds()),
                RLPItem.fromString(msg.getRemotePeer()),
                RLPItem.fromLong(msg.getTtl()),
                RLPItem.fromLong(msg.getNonce()),
                RLPItem.fromBytes(msg.getSignature().toByteArray()),
                RLPItem.fromBytes(msg.getBody().toByteArray())
        ).getEncoded().length;

        // 59
        System.out.println(sizeofRLP);
    }

    @Test
    public void test1() {
        String hex = "f901878080831edc6d94fd4c0928d654cd30a569402b1171ea5c140de5ef80b9012464561ec300000000000000000000000000000000000000000000000000000000000000a0000000000000000000000000dff6eb3a1965789f9d1ab95243ac9b9ceb1ad1d30000000000000000000000000000000000000000000000000de0b6b3a7640000000000000000000000000000000000000000000000000000000000000000000100000000000000000000000000000000000000000000000000000000000000e00000000000000000000000000000000000000000000000000000000000000006737472696e6700000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000001ff0000000000000000000000000000000000000000000000000000000000000081efa024edff7460426447170f666ff95727d6045e879ecf033e7709229e80c8a9ba9fa02f4e41340030ccd93dcb1d05f5031e6d42c21e08ee1113291844449c3ea3b1f8";

        Transaction t = new Transaction(ByteUtil.hexStringToBytes(hex));
        String sender = ByteUtil.toHexString(t.getSender());
        System.out.println(sender);
    }

    @Test
    public void test2() {
    }
}
