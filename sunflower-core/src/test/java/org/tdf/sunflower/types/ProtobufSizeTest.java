package org.tdf.sunflower.types;

import com.google.protobuf.ByteString;
import com.google.protobuf.Timestamp;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.tdf.rlp.RLPItem;
import org.tdf.rlp.RLPList;
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
}
