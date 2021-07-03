package org.tdf.sunflower.types

import org.tdf.common.types.Uint256
import org.tdf.common.util.HexBytes

class BlockValidateResult(
    success: Boolean, reason: String,
    val gas: Long = 0,
    val fee: Uint256 = Uint256.ZERO,
    val results: Map<HexBytes, VMResult> = emptyMap(),
    val infos: List<TransactionInfo> = emptyList()
) : ValidateResult(success, reason) {
    companion object {
        @JvmStatic
        fun fault(reason: String): BlockValidateResult {
            return BlockValidateResult(false, reason)
        }

        @JvmStatic
        fun success(
            gas: Long = 0,
            fee: Uint256 = Uint256.ZERO,
            results: Map<HexBytes, VMResult> = emptyMap(),
            infos: List<TransactionInfo> = emptyList()
        ): BlockValidateResult {
            return BlockValidateResult(true, "", gas, fee, results, infos)
        }
    }
}