package org.tdf.sunflower.p2pv2.eth.message

import org.tdf.common.util.HexBytes
import org.tdf.rlpstream.*
import org.tdf.sunflower.p2pv2.message.Message
import org.tdf.sunflower.types.TransactionReceipt
import java.math.BigInteger

abstract class EthMessage(command: EthMessageCodes) : Message(command)

@RlpProps("protocolVersion", "networkId", "totalDifficulty", "bestHash", "genesisHash", "forkId")
class StatusMessage(
    val protocolVersion: Int = 0,
    val networkId: Int = 0,
    val totalDifficulty: HexBytes = HexBytes.empty(),
    val bestHash: HexBytes = HexBytes.empty(),
    val genesisHash: HexBytes = HexBytes.empty(),
    val forkId: Array<HexBytes> = emptyArray()
) : EthMessage(EthMessageCodes.STATUS) {
    companion object {

        @JvmStatic
        @RlpCreator
        fun fromRlpStream(bin: ByteArray, streamId: Long): StatusMessage {
            val li = RlpList(bin, streamId, 6)
            return StatusMessage(
                li.intAt(0), li.intAt(1),
                HexBytes.fromBytes(li.bytesAt(2)),
                HexBytes.fromBytes(li.bytesAt(3)),
                HexBytes.fromBytes(li.bytesAt(4)),
                if (li.size() >= 6) li.valueAt(5, Array<HexBytes>::class.java) else emptyArray()
            )
        }
    }


    val totalDifficultyAsBigInt: BigInteger
        get() = BigInteger(1, totalDifficulty.bytes)

    override fun toString(): String {
        return "StatusMessage(protocolVersion=$protocolVersion, networkId=$networkId, totalDifficulty=$totalDifficulty, bestHash=$bestHash, genesisHash=$genesisHash, forkId=${forkId.contentToString()})"
    }
}

class ReceiptsMessage(val receipts: List<List<TransactionReceipt>>) : EthMessage(EthMessageCodes.RECEIPTS),
    RlpEncodable {
    override fun getEncoded(): ByteArray {
        val li = receipts.map {
            it.map { x -> x.getEncoded(true) }
        }
        val blocks = li.map { Rlp.encodeElements(it) }
        return Rlp.encodeElements(blocks)
    }

    companion object {

        @JvmStatic
        @RlpCreator
        fun fromRlpStream(bin: ByteArray, streamId: Long): ReceiptsMessage {
            val array = RlpStream.decode(bin, streamId, Array<Array<TransactionReceipt>>::class.java)
            return ReceiptsMessage(array.map { it.toList() })
        }
    }
}

