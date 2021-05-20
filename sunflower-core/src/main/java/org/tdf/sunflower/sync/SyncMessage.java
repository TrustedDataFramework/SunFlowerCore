package org.tdf.sunflower.sync;

import org.tdf.rlpstream.Rlp;
import org.tdf.rlpstream.RlpList;

import java.util.Optional;

public class SyncMessage {
    public static final int UNKNOWN = 0;

    public static final int STATUS = 4;
    public static final int GET_BLOCKS = 5;
    public static final int BLOCKS = 6;
    public static final int PROPOSAL = 7;
    public static final int TRANSACTION = 8;
    public static final int GET_ACCOUNTS = 11;
    public static final int ACCOUNTS = 12;

    private int code;

    public int getCode() {
        return code;
    }

    private byte[] rawBody;

    public SyncMessage(int code, byte[] rawBody) {
        this.code = code;
        this.rawBody = rawBody;
    }

    public SyncMessage() {
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

    public <T> T getBodyAs(Class<T> clazz) {
        return Rlp.decode(rawBody, clazz);
    }
}
