package org.tdf.sunflower.p2pv2.eth.message

import org.tdf.common.util.HexBytes
import org.tdf.rlpstream.Rlp
import org.tdf.sunflower.p2pv2.Loggers
import org.tdf.sunflower.p2pv2.eth.EthVersion
import org.tdf.sunflower.p2pv2.message.MessageDecoder

class EthMessageDecoder(val version: EthVersion) : MessageDecoder<EthMessageCodes>, Loggers {
    override fun decode(code: EthMessageCodes, encoded: ByteArray): EthMessage {
        dev.info("create eth message cmd = $code bin = ${HexBytes.encode(encoded)}")
        return when (code) {
            EthMessageCodes.STATUS -> Rlp.decode(encoded, StatusMessage::class.java)
            EthMessageCodes.RECEIPTS -> Rlp.decode(encoded, ReceiptsMessage::class.java)
            else -> throw RuntimeException("unknown command $code")
        }
    }
}