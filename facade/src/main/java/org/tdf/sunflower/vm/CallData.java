package org.tdf.sunflower.vm;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.tdf.common.types.Uint256;
import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.state.Address;
import org.tdf.sunflower.types.Transaction;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CallData {
    public static CallData empty() {
        return new CallData(
                Address.empty(),
                Uint256.ZERO,
                Uint256.ZERO,
                Address.empty(),
                Address.empty(),
                Transaction.Type.CONTRACT_CALL.code,
                Transaction.Type.CONTRACT_CALL.code,
                HexBytes.empty(),
                Address.empty(),
                HexBytes.empty(),
                0,
                HexBytes.empty(),
                0,
                Uint256.ZERO,
                0
        );
    }

    public static CallData fromTransaction(Transaction tx) {
        return new CallData(
                tx.isCoinbase() ? Address.empty() : tx.getFromAddress(),
                tx.getAmount(),
                tx.getAmount(),
                tx.getTo(),
                tx.getTo(),
                tx.getType(),
                tx.getType(),
                tx.getPayload(),
                tx.isCoinbase() ? Address.empty() : tx.getFromAddress(),
                tx.getHash(),
                tx.getNonce(),
                tx.getSignature(),
                tx.getCreatedAt(),
                tx.getGasPrice(),
                tx.getGasLimit()
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
    private HexBytes txHash;
    private long txNonce;
    private HexBytes txSignature;
    private long txCreatedAt;
    private Uint256 gasPrice;
    private long gasLimit;

    public CallData clone() {
        return new CallData(
                caller, amount, txAmount, to, txTo,
                txType, callType, payload, origin, txHash,
                txNonce, txSignature, txCreatedAt,
                gasPrice, gasLimit
        );
    }
}
