package org.tdf.common.store

import org.tdf.common.util.HexBytes

internal fun ByteArray.hex(): HexBytes {
    return HexBytes.fromBytes(this)
}