package org.tdf.sunflower.vm

import org.tdf.common.types.Uint256
import org.tdf.common.util.ByteUtil
import org.tdf.common.util.HexBytes
import org.tdf.sunflower.state.Address
import org.tdf.sunflower.types.Transaction


data class CallData(
    var caller: HexBytes = Address.empty(),
    var value: Uint256 = Uint256.ZERO,
    var txValue: Uint256 = Uint256.ZERO,
    var to: HexBytes = Address.empty(),
    var txTo: HexBytes = Address.empty(),
    var callType: CallType = CallType.COINBASE,
    var data: HexBytes = HexBytes.empty(),
    var origin: HexBytes = Address.empty(),
    var txHash: HexBytes = Address.empty(),
    var txNonce: Long = 0,
    var gasPrice: Uint256 = Uint256.ZERO,
    var gasLimit: Uint256 = Uint256.ZERO,
) {

    val txNonceAsBytes: ByteArray
        get() = ByteUtil.longToBytesNoLeadZeroes(txNonce)

    fun clone(): CallData {
        return this.copy()
    }

    companion object {
        @JvmStatic
        fun empty(): CallData {
            return CallData(
            )
        }

        @JvmStatic
        fun fromTransaction(tx: Transaction, coinbase: Boolean): CallData {
            var t = CallType.COINBASE
            val origin = if (coinbase) Address.empty() else tx.senderHex
            if (!coinbase) {
                t = if (tx.receiveHex.isEmpty) CallType.CREATE else CallType.CALL
            }
            return CallData(
                origin,
                tx.valueAsUint,
                tx.valueAsUint,
                tx.receiveHex,
                tx.receiveHex,
                t,
                tx.dataHex,
                origin,
                tx.hashHex,
                tx.nonceAsLong,
                tx.gasPriceAsU256,
                tx.gasLimitAsU256
            )
        }
    }
}