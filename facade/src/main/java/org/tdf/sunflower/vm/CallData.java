package org.tdf.sunflower.vm;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.tdf.common.types.Uint256;
import org.tdf.common.util.ByteUtil;
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
                CallType.COINBASE,
                HexBytes.empty(),
                Address.empty(),
                HexBytes.empty(),
                0,
                Uint256.ZERO,
                Uint256.ZERO
        );
    }

    public static CallData fromTransaction(Transaction tx, boolean coinbase) {
        CallData data = new CallData(
                tx.getSenderHex(),
                tx.getValueAsUint(),
                tx.getValueAsUint(),
                tx.getReceiveHex(),
                tx.getReceiveHex(),
                CallType.COINBASE,
                tx.getDataHex(),
                tx.getSenderHex(),
                tx.getHashHex(),
                tx.getNonceAsLong(),
                tx.getGasPriceAsU256(),
                tx.getGasLimitAsU256()
        );

        if(!coinbase){
            data.setCallType(tx.getReceiveHex().isEmpty() ? CallType.CREATE : CallType.CALL);
        } else {
            // for coinbase tx without v,r,s,
            // set origin as zero address
            data.setOrigin(Address.empty());
            data.setCaller(Address.empty());
        }
        return data;
    }


    private HexBytes caller;
    private Uint256 value;
    private Uint256 txValue;
    private HexBytes to;
    private HexBytes txTo;
    private CallType callType;
    private HexBytes data;
    private HexBytes origin;
    private HexBytes txHash;
    private long txNonce;
    private Uint256 gasPrice;
    private Uint256 gasLimit;

    public byte[] getTxNonceAsBytes() {
        return
                ByteUtil.longToBytesNoLeadZeroes(txNonce);
    }

    public CallData clone() {
        return new CallData(
                caller, value, txValue, to, txTo,
                callType, data, origin, txHash, txNonce,
                gasPrice, gasLimit
        );
    }
}
