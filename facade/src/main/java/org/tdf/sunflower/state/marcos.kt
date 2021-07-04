package org.tdf.sunflower.state

import org.tdf.common.util.HexBytes
import java.nio.charset.StandardCharsets

internal fun ByteArray.hex(): HexBytes{
    return HexBytes.fromBytes(this)
}

internal fun String.hex(): HexBytes {
    return HexBytes.fromHex(this)
}

internal fun String.ascii(): HexBytes {
    return HexBytes.fromBytes(this.toByteArray(StandardCharsets.US_ASCII))
}