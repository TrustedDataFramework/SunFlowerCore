package org.tdf.sunflower.sync;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.tdf.rlp.RLPCodec;
import org.tdf.rlp.RLPElement;
import org.tdf.rlp.RLPList;

import java.util.Optional;

@NoArgsConstructor
@AllArgsConstructor
@Data
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

    private RLPElement body;

    public static byte[] encode(int code, Object msg) {
        return RLPCodec.encode(new Object[]{code, msg});
    }

    public static Optional<SyncMessage> decode(byte[] rlp) {
        RLPList li = RLPElement.fromEncoded(rlp).asRLPList();
        int code = li.get(0).asInt();
        if (code < STATUS) return Optional.empty();
        return Optional.of(li.as(SyncMessage.class));
    }

    public <T> T getBodyAs(Class<T> clazz) {
        return body.as(clazz);
    }
}
