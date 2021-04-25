package org.tdf.sunflower.vm;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.tdf.common.types.Uint256;
import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.types.Transaction;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CallDataImpl implements CallData{
    public static CallDataImpl fromTransaction(Transaction tx) {
        return new CallDataImpl(
                tx.getFromAddress(),
                tx.getAmount(),
                tx.getAmount(),
                tx.getTo(),
                tx.getTo(),
                tx.getType(),
                tx.getType(),
                tx.getPayload(),
                tx.getFromAddress(),
                tx.getHash(),
                tx.getNonce(),
                tx.getSignature(),
                tx.getCreatedAt(),
                false
        );
    }


    private HexBytes caller;
    private Uint256 amount;
    private Uint256 txAmount;
    private HexBytes to;
    private HexBytes txTo;
    private int txType;
    private int callType;
    private HexBytes payload;
    private HexBytes origin;
    private HexBytes  txHash;
    private long txNonce;
    private HexBytes txSignature;
    private long txCreatedAt;
    private boolean isStatic;

    public boolean isStatic() {
        return isStatic;
    }

    public void setStatic(boolean isStatic){
        this.isStatic = isStatic;
    }

    public CallDataImpl clone() {
        return new CallDataImpl(
                caller, amount, txAmount, to, txTo,
                txType, callType, payload, origin, txHash,
                txNonce, txSignature, txCreatedAt,
                isStatic
        );
    }
}
