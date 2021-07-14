package org.tdf.sunflower.abi

import org.tdf.common.util.BigEndian
import org.tdf.common.util.ascii
import org.tdf.common.util.sha3

internal fun String.selector(): Int {
    return BigEndian.decodeInt32(this.ascii().sha3(), 0)
}

enum class SolidityType(val selector: Int) {
    Address("Address".selector()),
}