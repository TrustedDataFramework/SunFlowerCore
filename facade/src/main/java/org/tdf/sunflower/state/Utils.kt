package org.tdf.sunflower.state

import org.tdf.common.util.HexBytes
import org.tdf.sunflower.vm.CallData

object Utils {
    fun createCallData(encoded: ByteArray): CallData {
        return CallData(data = HexBytes.fromBytes(encoded))
    }
}
