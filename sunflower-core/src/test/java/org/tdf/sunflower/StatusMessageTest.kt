package org.tdf.sunflower

import com.github.salpadding.rlpstream.Rlp
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.tdf.common.types.Uint256
import org.tdf.common.util.HexBytes
import org.tdf.common.util.hex
import org.tdf.sunflower.p2pv2.eth.message.StatusMessage
import org.tdf.sunflower.types.Transaction
import java.math.BigInteger

@RunWith(JUnit4::class)
class StatusMessageTest {

    @Test
    fun test() {
        val hex =
            "f84f410183c465d2a07a617029159fbeb2c95ace4cda8df778f61d3052d0a64838a625b79c1649ccd5a04fc24894912ef096ebe80dc15601c3bdf967d852c433e762b585d8ad8856cad3c68439c50cdd80";
        val status = Rlp.decode(HexBytes.decode(hex), StatusMessage::class.java)
        println("status = $status")
    }

    @Test
    fun test1() {
        val tx = Transaction(nonce=20, gasPrice= Uint256.ZERO, gasLimit=2000000, to="bd8156c26c1b048bea6e085600444b66a2a87629".hex(), value= Uint256.of("1000000000000000000"), data="b214faa5132fb44ad2a35cba64cc363dfb68c52525c24122c7f7f55386e7a86e64fd6257".hex(), vrs=Triple(
            BigInteger.valueOf(239), "f1dbadda31e56bd4003c86fc9cd600c32c8eabe0ad2f9507c33f3640bed0b800".hex(), "3f6636105794f8ff3c561168860915ebbbdbb9de763f737db2253274c32d86e8".hex()))
                
    }
}