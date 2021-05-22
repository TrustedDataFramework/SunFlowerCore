package org.tdf.sunflower

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.tdf.common.util.HexBytes
import org.tdf.rlpstream.Rlp
import org.tdf.sunflower.p2pv2.P2pMessageCodes
import org.tdf.sunflower.p2pv2.client.Capability
import org.tdf.sunflower.p2pv2.eth.EthVersion
import org.tdf.sunflower.p2pv2.p2p.HelloMessage

@RunWith(JUnit4::class)
class HelloMessageTest {

    @Test
    fun test0() {
        val hello = Rlp.decode(
            HexBytes.decode("f87902a5457468657265756d282b2b292f76302e372e392f52656c656173652f4c696e75782f672b2bccc58365746827c583736868018203e0b8401fbf1e41f08078918c9f7b6734594ee56d7f538614f602c71194db0a1af5a77f9b86eb14669fe7a8a46a2dd1b7d070b94e463f4ecd5b337c8b4d31bbf8dd5646"),
            HelloMessage::class.java
        )

        assertEquals(2, hello.p2pVersion)
        assertEquals("Ethereum(++)/v0.7.9/Release/Linux/g++", hello.clientId)
        assertEquals(2, hello.capabilities.size)
        assertEquals(992, hello.listenPort)
        assertEquals(
            "1fbf1e41f08078918c9f7b6734594ee56d7f538614f602c71194db0a1af5a77f9b86eb14669fe7a8a46a2dd1b7d070b94e463f4ecd5b337c8b4d31bbf8dd5646",
            hello.peerIdHex
        )
    }

    @Test
    fun test1() {
        //Init
        val version = 2
        val clientStr = "Ethereum(++)/v0.7.9/Release/Linux/g++"
        val capabilities: List<Capability> = listOf(
            Capability(Capability.ETH, EthVersion.UPPER),
            Capability(Capability.SHH, 3),
            Capability(Capability.P2P, 5)
        )
        val listenPort = 992
        val peerId =
            "f6334e3488a58bc9c9ff9acefaef88255b511a6b68ea2bebccbc1f9e608317f378d7db42ddf2b053ec59f32d88c2409291bd61085caf553564d82db320e67170"

        val helloMessage =
            HelloMessage(version, clientStr, capabilities.toTypedArray(), listenPort, HexBytes.decode(peerId))

        assertEquals(P2pMessageCodes.HELLO, helloMessage.command)
        assertEquals(version, helloMessage.p2pVersion)
        assertEquals(clientStr, helloMessage.clientId)
        assertEquals(3, helloMessage.capabilities.size)
        assertEquals(listenPort, helloMessage.listenPort)
        assertEquals(peerId, helloMessage.peerIdHex)
    }
}