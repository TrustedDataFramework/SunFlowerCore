package org.tdf.sunflower.p2pv2.eth.message

import org.tdf.common.util.ByteUtil
import org.tdf.rlpstream.RlpProps
import org.tdf.sunflower.p2pv2.message.Message
import java.math.BigInteger

sealed class EthMessage(command: EthMessageCodes): Message(command)

@RlpProps("protocolVersion", "networkId", "totalDifficulty", "bestHash", "genesisHash")
class StatusMessage : EthMessage(EthMessageCodes.STATUS) {
    var protocolVersion: Int = 0
    var networkId: Int = 0
    var totalDifficulty: ByteArray = ByteUtil.EMPTY_BYTE_ARRAY
    var bestHash: ByteArray = ByteUtil.EMPTY_BYTE_ARRAY
    var genesisHash: ByteArray  = ByteUtil.EMPTY_BYTE_ARRAY

    val totalDifficultyAsBigInt: BigInteger
        get() = BigInteger(1, totalDifficulty)

}

