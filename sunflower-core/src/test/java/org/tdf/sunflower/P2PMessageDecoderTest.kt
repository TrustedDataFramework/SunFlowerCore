package org.tdf.sunflower

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.tdf.common.util.HexBytes
import org.tdf.sunflower.p2pv2.P2pMessageCodes
import org.tdf.sunflower.p2pv2.message.Message
import org.tdf.sunflower.p2pv2.p2p.HelloMessage
import org.tdf.sunflower.p2pv2.p2p.P2PMessageDecoder.decode

@RunWith(JUnit4::class)
class P2PMessageDecoderTest {
    @Test
    fun test0() {
        val bin =
            "f8540580ccc5836574683ec5836574683f822161b840db477274e444681881172d4a7ce2ff1d00403e5dd6e8d9341c6fb7a448115755665ccef8acc7da3c56d7d0949ae22cf7bc19fad8ffbe3563e02356be834502cb"
        val msg: Message = decode(P2pMessageCodes.HELLO, HexBytes.decode(bin))
        assert(msg is HelloMessage)
        val m = msg as HelloMessage
        assert(m.listenPort == 8545)
    }
}