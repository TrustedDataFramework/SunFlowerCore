package org.tdf.sunflower

import org.spongycastle.util.encoders.Hex
import org.tdf.rlpstream.Rlp
import org.tdf.rlpstream.RlpCreator
import org.tdf.rlpstream.RlpProps
import java.math.BigInteger
import java.util.*

@RlpProps("chainId", "address", "poolType", "f", "fm", "r", "rm", "debt", "price", "decimals")
class PoolData @RlpCreator constructor(
    val chainId: Long = 0,
    val address: ByteArray,
    val poolType: Long = 0,
    val f: BigInteger? = null,
    val fm: BigInteger? = null,
    val r: BigInteger? = null,
    val rm: BigInteger? = null,
    val debt: BigInteger? = null,
    val price: BigInteger? = null,
    val decimals: Long = 0,
) {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val dt =
                "f90205f84e820539942c93e2f9f75382717af5de4c105ffb4c6503c5b4038a01605d9ee98627100000891b1ae4d6e2ef50000089f3f20b8dfa69d00000891b1ae4d6e2ef5000008089020281c283b028524012f182053994eb4d5af9f8cbb97f6eb95c21f2ff541b121c7fd1018814d1120d7b160000808080808923b97412d86c4ea13a12f84d8205399444915ecba748148cf6ad6a323af8be52d3befb8f01890ad78ebc5ac6200000890ad78ebc5ac6200000890ad78ebc5ac6200000890ad78ebc5ac62000008089d5c457fd13c65daff712f84e8205399434451604347d45ef4b5cbd790e88d09907b1706c0189055005f0c61448000089055005f0c6144800008915af1d78b58c4000008915af1d78b58c400000808a0df94d0efa177fd1a51812f8508205399438e4f0437edd9bda6f32caae007c985b97bbcff1808a01a46d2eef9995fe00008a010ec78cd35b142c00008a010f0cf064dd592000008a010f0cf064dd5920000080880de0b6b3a764000012f85082053994c7376932e8f7f03d33ffb3ed781d7f28c6c5bbb5808a01c37637845d6d2000008a010ec78cd35b142c00008a0202fefbf2d7c2f000008a010f0cf064dd5920000080880de0b6b3a764000012f83e820539945b536881e3c4fd7639ca0dcaeffcd73daff98523028a021e19e0c9bab24000008a021e19e0c9bab24000008a021e19e0c9bab240000080808012"
            val bytes = Hex.decode(dt)

            val datas: Array<PoolData> = Rlp.decode(
                bytes,
                Array<PoolData>::class.java
            )

            val encoded = Rlp.encode(datas)
            print(bytes.contentEquals(encoded))
        }
    }
}