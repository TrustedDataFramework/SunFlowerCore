package org.tdf.sunflower.sync;

import com.github.salpadding.rlpstream.*;
import org.tdf.common.util.ByteUtil;
import java.util.Optional;

public class SyncMessage implements RlpWritable {
    public static final int UNKNOWN = 0;

    public static final int STATUS = 4;
    public static final int GET_BLOCKS = 5;
    public static final int BLOCKS = 6;
    public static final int PROPOSAL = 7;
    public static final int TRANSACTION = 8;
    public static final int GET_ACCOUNTS = 11;
    public static final int ACCOUNTS = 12;

    private int code;
    private byte[] rawBody;

    public SyncMessage(int code, byte[] rawBody) {
        this.code = code;
        this.rawBody = rawBody;
    }

    @RlpCreator
    public static SyncMessage fromRlpStream(byte[] bin, long streamId) {
        RlpList li = new RlpList(bin, streamId, 2);
        SyncMessage msg = new SyncMessage(0, ByteUtil.EMPTY_BYTE_ARRAY);
        msg.code = li.intAt(0);
        msg.rawBody = li.rawAt(1);
        return msg;
    }

    public static byte[] encode(int code, Object msg) {
        return Rlp.encode(new Object[]{code, msg});
    }

    public static Optional<SyncMessage> decode(byte[] rlp) {
        RlpList li = Rlp.decodeList(rlp);
        int code = li.intAt(0);
        if (code < STATUS) return Optional.empty();
        return Optional.of(li.as(SyncMessage.class));
    }

    public int getCode() {
        return code;
    }

    public <T> T getBodyAs(Class<T> clazz) {
        return Rlp.decode(rawBody, clazz);
    }

    @Override
    public int writeToBuf(RlpBuffer rlpBuffer) {
        return rlpBuffer.writeElements(Rlp.encodeInt(code), rawBody);
    }
}
