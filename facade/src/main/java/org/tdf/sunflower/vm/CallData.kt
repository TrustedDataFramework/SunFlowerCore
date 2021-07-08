package org.tdf.sunflower.vm

import org.tdf.common.types.Uint256
import org.tdf.common.util.HashUtil
import org.tdf.common.util.HexBytes
import org.tdf.sunflower.state.Address
import org.tdf.sunflower.types.Transaction

data class CallContext(
    val origin: HexBytes = Address.empty(),
    val txHash: HexBytes = HashUtil.EMPTY_DATA_HASH_HEX,
    val txNonce: Long = 0,
    val gasPrice: Uint256 = Uint256.ZERO,
    val chainId: Int = 0,
) {
    companion object {
        @JvmStatic
        fun fromTx(tx: Transaction, chainId: Int = tx.chainId ?: 0): CallContext {
            return CallContext(
                tx.sender,
                tx.hash,
                tx.nonce,
                tx.gasPrice,
                chainId
            )
        }
    }
}

data class CallData(
    val caller: HexBytes = Address.empty(),
    val value: Uint256 = Uint256.ZERO,
    val to: HexBytes = Address.empty(),
    val callType: CallType = CallType.COINBASE,
    val data: HexBytes = HexBytes.empty(),
    val delegateAddr: HexBytes = Address.empty()
) {

    fun clone(): CallData {
        return this.copy()
    }

    companion object {
        @JvmStatic
        fun empty(): CallData {
            return CallData()
        }

        @JvmStatic
        fun fromTx(tx: Transaction, coinbase: Boolean): CallData {
            var t = CallType.COINBASE
            val origin = if (coinbase) Address.empty() else tx.sender
            if (!coinbase) {
                t = if (tx.to.isEmpty()) CallType.CREATE else CallType.CALL
            }
            return CallData(
                origin,
                tx.value,
                tx.to,
                t,
                tx.data,
            )
        }
    }
}