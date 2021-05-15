package org.tdf.sunflower.sync;

import org.tdf.rlp.RLPCodec;
import org.tdf.rlp.RLPElement;
import org.tdf.rlp.RLPList;

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

    private RLPElement body;

    public SyncMessage(int code, RLPElement body) {
        this.code = code;
        this.body = body;
    }

    public SyncMessage() {
    }

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

    public int getCode() {
        return this.code;
    }

    public RLPElement getBody() {
        return this.body;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public void setBody(RLPElement body) {
        this.body = body;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof SyncMessage)) return false;
        final SyncMessage other = (SyncMessage) o;
        if (!other.canEqual((Object) this)) return false;
        if (this.getCode() != other.getCode()) return false;
        final Object this$body = this.getBody();
        final Object other$body = other.getBody();
        if (this$body == null ? other$body != null : !this$body.equals(other$body)) return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof SyncMessage;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        result = result * PRIME + this.getCode();
        final Object $body = this.getBody();
        result = result * PRIME + ($body == null ? 43 : $body.hashCode());
        return result;
    }

    public String toString() {
        return "SyncMessage(code=" + this.getCode() + ", body=" + this.getBody() + ")";
    }
}
