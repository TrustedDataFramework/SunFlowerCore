package org.tdf.sunflower.controller

import org.tdf.common.types.Uint256
import org.tdf.common.util.HashUtil
import org.tdf.common.util.HexBytes
import org.tdf.sunflower.controller.TypeConverter.*
import org.tdf.sunflower.state.Address
import org.tdf.sunflower.vm.CallContext
import org.tdf.sunflower.vm.CallData
import org.tdf.sunflower.vm.CallType


internal object JsonRpcUtil {
    private fun String?.notEmptyOrNull(): String? {
        if (this == null || this.trim().isEmpty())
            return null
        return this.trim()
    }

    private fun String?.number(): Long {
        return this.notEmptyOrNull()?.let { jsonHexToLong(it) } ?: 0L
    }

    private fun String?.address(): HexBytes {
        return this.notEmptyOrNull()?.let { jsonHexToHexBytes(it) } ?: Address.empty()
    }

    private fun String?.u256(): Uint256 {
        return this.notEmptyOrNull()?.let { jsonHexToU256(it) } ?: Uint256.ZERO
    }

    private fun String?.hex(): HexBytes {
        return this.notEmptyOrNull()?.let { jsonHexToHexBytes(it) } ?: HexBytes.empty()
    }

    fun toCallContext(args: JsonRpc.CallArguments, nonce: Long?): CallContext {
        return CallContext(
            args.from.address(),
            HashUtil.EMPTY_DATA_HASH_HEX,
            nonce ?: args.nonce.number(),
            args.gasPrice.u256(),
            args.gas.u256(),
        )
    }

    fun toCallData(args: JsonRpc.CallArguments): CallData {
        return CallData(
            args.from.address(),
            args.value.u256(),
            args.to.address(),
            if (args.to == null || args.to.trim().isEmpty()) {
                CallType.CREATE
            } else {
                CallType.CALL
            },
            args.data.hex(),
        )
    }
}