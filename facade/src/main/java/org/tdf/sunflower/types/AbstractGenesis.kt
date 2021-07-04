package org.tdf.sunflower.types

import com.fasterxml.jackson.databind.JsonNode
import org.tdf.common.types.Uint256
import org.tdf.common.util.ByteUtil
import org.tdf.common.util.HexBytes
import org.tdf.sunflower.state.Account
import org.tdf.sunflower.state.Account.Companion.emptyAccount
import java.math.BigInteger

internal fun String.hex(): HexBytes {
    return HexBytes.fromHex(this)
}

internal fun String.bn(): BigInteger {
    return if (this.startsWith("0x")) BigInteger(this.substring(2), 16) else BigInteger(this)
}

abstract class AbstractGenesis(protected var parsed: JsonNode) {
    abstract val block: Block?

    val timestamp: Long
        get() = parsed["timestamp"]?.asLong() ?: 0L

    val parentHash: HexBytes
        get() = parsed["gasLimit"]?.asText()?.hex() ?: ByteUtil.ZEROS_32

    val gasLimit: Long
        get() = parsed["gasLimit"]?.asLong() ?: 0

    val gasLimitHex: HexBytes
        get() = HexBytes.fromBytes(ByteUtil.longToBytesNoLeadZeroes(gasLimit))

    val alloc: Map<HexBytes, Account>
        get() {
            val alloc = parsed["alloc"] ?: return emptyMap()
            val r: MutableMap<HexBytes, Account> = HashMap()
            val it = alloc.fieldNames()
            while (it.hasNext()) {
                val k = it.next()
                val v = alloc[k].asText()
                val balance = Uint256.of(v.bn())
                r[k.hex()] = emptyAccount(balance)
            }
            return r
        }

    protected fun getArray(field: String): List<JsonNode> {
        val n = parsed[field]
        if (n == null || n.isNull) return emptyList()
        val li: MutableList<JsonNode> = ArrayList()
        for (i in 0 until n.size()) {
            li.add(n[i])
        }
        return li
    }
}