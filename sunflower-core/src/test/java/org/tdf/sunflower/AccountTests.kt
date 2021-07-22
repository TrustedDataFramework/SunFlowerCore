package org.tdf.sunflower

import com.github.salpadding.rlpstream.Rlp
import com.github.salpadding.rlpstream.annotation.RlpCreator
import com.github.salpadding.rlpstream.annotation.RlpProps
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.spongycastle.util.encoders.Hex
import org.tdf.common.types.Uint256
import org.tdf.common.util.*
import org.tdf.sunflower.controller.bytes
import org.tdf.sunflower.state.Account
import org.tdf.sunflower.types.Bloom
import org.tdf.sunflower.types.LogFilter
import org.tdf.sunflower.types.Transaction
import org.tdf.sunflower.types.VRS
import java.math.BigInteger

@RunWith(JUnit4::class)
class AccountTests {

    @Test
    fun testGetEncoded() {
        val expected = ("f85e809"
                + "a0100000000000000000000000000000000000000000000000000"
                + "a056e81f171bcc55a6ff8345e692c0f86e5b48e01b996cadc001622fb5e363b421"
                + "a0c5d2460186f7233c927e7db2dcc703c0e500b653ca82273b7bfad8045d85a470")
        val acct = Account(balance = Uint256.Companion.of(BigInteger.valueOf(2).pow(200)))
        Assert.assertEquals(expected, Hex.toHexString(Rlp.encode(acct)))
        val decoded = Rlp.decode(HexBytes.decode(expected), Account::class.java)
        assert(decoded == acct)
    }

    @Test
    fun testGetEncoded1() {
        val expected = ("f85e809"
                + "a0100000000000000000000000000000000000000000000000000"
                + "a056e81f171bcc55a6ff8345e692c0f86e5b48e01b996cadc001622fb5e363b421"
                + "a0c5d2460186f7233c927e7db2dcc703c0e500b653ca82273b7bfad8045d85a470")
        val acct = AccountJ.emptyAccount(Uint256.Companion.of(BigInteger.valueOf(2).pow(200)))
        Assert.assertEquals(expected, Hex.toHexString(Rlp.encode(acct)))
    }

    @RlpProps("n")
    data class Foo @RlpCreator constructor(val n: String = "")

    @Test
    fun testDefault() {
        val s = "0xc483616263"
        val f = Rlp.decode(HexBytes.decode(s), Foo::class.java)
        println(f)
    }

    @Test
    fun testFilter() {
        val bloom = "00000000000000040000000000000000000000000400000000000000000080000000000000000000000000000000000000000000000000000000000000000000000000000000100000000000800000000000000000000000000000000000000000000000020000000000000000000800000000000000000000000000000000000000000000000000000400000000000000000000000000000000000000000000000001000000000000000000000000000000000001000000000000000000000000000000000000000000000000000000000000000000000000000000000020000000000000000000000000000000000000000000000010000000000000000000".hex()
        val f = LogFilter()
        f
            .withContractAddress("0xf8B2d82d74c83A6A0e2A6FA9823B30508b987277".address().bytes)
            .withTopic("0x342827c97908e5e2f71151c08502a66d44b6f758e3ac2f1de95f02eb95f0a735".hex().bytes)

        println(f.matchBloom(Bloom(bloom.bytes)))
    }

    @Test
    fun test3() {
        val tx = Transaction(
            nonce=381,
            gasPrice= "20000000000".u256(),
            gasLimit=6721975, to= "".hex(), value= "0".u256(),
            data="608060405234801561001057600080fd5b50600080546001600160a01b031916331790556101b3806100326000396000f3fe608060405234801561001057600080fd5b506004361061004c5760003560e01c80630900f01014610051578063445df0ac146100795780638da5cb5b14610093578063fdacd576146100b7575b600080fd5b6100776004803603602081101561006757600080fd5b50356001600160a01b03166100d4565b005b610081610151565b60408051918252519081900360200190f35b61009b610157565b604080516001600160a01b039092168252519081900360200190f35b610077600480360360208110156100cd57600080fd5b5035610166565b6000546001600160a01b031633141561014e576000819050806001600160a01b031663fdacd5766001546040518263ffffffff1660e01b815260040180828152602001915050600060405180830381600087803b15801561013457600080fd5b505af1158015610148573d6000803e3d6000fd5b50505050505b50565b60015481565b6000546001600160a01b031681565b6000546001600160a01b031633141561014e5760015556fea265627a7a7231582063fc9209a78975378d8af204b7e01b5ee66e336daa41159cbd14d6ee0a6786c664736f6c63430005110032".hex(),
            vrs= VRS(BigInteger.valueOf(27), "40160d35ec1ed50a4a3fe938096b284b59a0547a4e3c3768bd5c2844798afbd8".hex(), "401348fbe5e8f2a0e63a55bec415939e4891ba340d775ee214d2c95b2c0b5cb0".hex())
        )

        println(tx.chainId)
    }
}