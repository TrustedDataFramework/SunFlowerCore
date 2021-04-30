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
                0,
                0,
                HexBytes.empty(),
                Address.empty(),
                HexBytes.empty(),
                0,
                0,
                Uint256.ZERO,
                0
        );
    }

    public static CallData fromTransaction(Transaction tx) {
        return empty();
    }


    private HexBytes caller;
    private Uint256 amount;
    private Uint256 txAmount;
    private HexBytes to;
    private HexBytes txTo;
    private int txType;
    private int callType;
    private HexBytes data;
    private HexBytes origin;
    private HexBytes txHash;
    private long txNonce;
    private long txCreatedAt;
    private Uint256 gasPrice;
    private long gasLimit;

    public CallData clone() {
        return new CallData(
                caller, amount, txAmount, to, txTo,
                txType, callType, data, origin, txHash,
                txNonce, txCreatedAt,
                gasPrice, gasLimit
        );
    }
}
