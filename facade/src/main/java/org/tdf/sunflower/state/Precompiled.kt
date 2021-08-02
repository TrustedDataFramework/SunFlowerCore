package org.tdf.sunflower.state

import org.tdf.common.crypto.ECDSASignature
import org.tdf.common.crypto.ECKey
import org.tdf.common.crypto.zksnark.BN128
import org.tdf.common.crypto.zksnark.BN128Fp
import org.tdf.common.crypto.zksnark.Fp
import org.tdf.common.util.*
import org.tdf.common.util.ByteUtil.stripLeadingZeroes

fun validateV(v: ByteArray): Boolean {
    for (i in 0 until v.size - 1) {
        if (v[i] != (0).toByte()) {
            return false
        }
    }
    return true
}

class ECRecover: Precompiled {
    override val address: HexBytes = "0000000000000000000000000000000000000000000000000000000000000001".hex()

    override fun execute(data: ByteArray): ByteArray {
        val h = ByteArray(32)
        val v = ByteArray(32)
        val r = ByteArray(32)
        val s = ByteArray(32)


        System.arraycopy(data, 0, h, 0, 32)
        System.arraycopy(data, 32, v, 0, 32)
        System.arraycopy(data, 64, r, 0, 32)
        val sLength = if (data.size < 128) data.size - 96 else 32
        System.arraycopy(data, 96, s, 0, sLength)
        val signature: ECDSASignature = ECDSASignature.fromComponents(r, s, v[31])
        if (validateV(v) && signature.validateComponents()) {
            val bytes = ByteArray(32)
            System.arraycopy(ECKey.signatureToAddress(h, signature), 0, bytes, 32 - 20, 20);
            return bytes
        }
        throw RuntimeException("erc recover failed")
    }
}

interface Precompiled {
    companion object {
        val PRECOMPILED: MutableMap<HexBytes, Precompiled> = mutableMapOf()

        init {
            val contracts = listOf(
                ECRecover(),
                BN128Multiplication()
            )

            contracts.forEach {
                PRECOMPILED[it.address] = it
            }
        }
    }

    val address: Address
    fun execute(data: ByteArray): ByteArray
}


/**
 * Computes multiplication of scalar value on a point belonging to Barreto–Naehrig curve.
 * See [BN128Fp] for details<br></br>
 * <br></br>
 *
 * input data[]:<br></br>
 * point encoded as (x, y) is followed by scalar s, where x, y and s are 32-byte left-padded integers,<br></br>
 * if input is shorter than expected, it's assumed to be right-padded with zero bytes<br></br>
 * <br></br>
 *
 * output:<br></br>
 * resulting point (x', y'), where x and y encoded as 32-byte left-padded integers<br></br>
 *
 */
class BN128Multiplication : Precompiled {
    override val address: Address
        get() = "0000000000000000000000000000000000000000000000000000000000000007".hex()

    override fun execute(data: ByteArray): ByteArray {
        val x: ByteArray = parseWord(data, 0)
        val y: ByteArray = parseWord(data, 1)
        val s: ByteArray = parseWord(data, 2)
        val p: BN128<Fp> = BN128Fp.create(x, y)
            ?: throw RuntimeException("create bn 128 failed")
        val res: BN128<Fp> = p.mul(s.bn()).toEthNotation()
        return encodeRes(res.x().bytes(), res.y().bytes())
    }
}


/**
 * Parses fixed number of bytes starting from `offset` in `input` array.
 * If `input` has not enough bytes return array will be right padded with zero bytes.
 * I.e. if `offset` is higher than `input.length` then zero byte array of length `len` will be returned
 */
fun parseBytes(input: ByteArray, offset: Int, len: Int): ByteArray {
    if (offset >= input.size || len == 0) return ByteUtil.EMPTY_BYTE_ARRAY
    val bytes = ByteArray(len)
    System.arraycopy(input, offset, bytes, 0, Math.min(input.size - offset, len))
    return bytes
}

/**
 * Parses 32-bytes word from given input.
 * Uses [.parseBytes] method,
 * thus, result will be right-padded with zero bytes if there is not enough bytes in `input`
 *
 * @param idx an index of the word starting from `0`
 */
fun parseWord(input: ByteArray, idx: Int): ByteArray {
    return parseBytes(input, 32 * idx, 32)
}

private fun encodeRes(w1: ByteArray, w2: ByteArray): ByteArray {
    var w1 = w1
    var w2 = w2
    val res = ByteArray(64)
    w1 = stripLeadingZeroes(w1)
    w2 = stripLeadingZeroes(w2)
    System.arraycopy(w1, 0, res, 32 - w1.size, w1.size)
    System.arraycopy(w2, 0, res, 64 - w2.size, w2.size)
    return res
}
