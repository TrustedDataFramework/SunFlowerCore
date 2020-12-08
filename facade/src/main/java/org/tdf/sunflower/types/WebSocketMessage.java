package org.tdf.sunflower.types;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.tdf.rlp.RLPElement;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WebSocketMessage {
    private long nonce;
    private int code;
    private RLPElement body;

    public Code getCodeEnum() {
        return Code.values()[code];
    }

    public enum Code {
        NULL,
        EVENT_EMIT,
        EVENT_SUBSCRIBE,
        TRANSACTION_EMIT,
        TRANSACTION_SUBSCRIBE,
        TRANSACTION_SEND,
        ACCOUNT_QUERY,
        CONTRACT_QUERY,
        ERROR
    }
}
