package org.tdf.sunflower

import com.github.salpadding.rlpstream.Rlp
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.tdf.common.util.HexBytes
import org.tdf.sunflower.p2pv2.eth.message.StatusMessage

@RunWith(JUnit4::class)
class StatusMessageTest {

    @Test
    fun test() {
        val hex =
            "f84f410183c465d2a07a617029159fbeb2c95ace4cda8df778f61d3052d0a64838a625b79c1649ccd5a04fc24894912ef096ebe80dc15601c3bdf967d852c433e762b585d8ad8856cad3c68439c50cdd80";
        val status = Rlp.decode(HexBytes.decode(hex), StatusMessage::class.java)
        println("status = $status")
    }
}