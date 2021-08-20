package org.tdf.natives

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.tdf.common.util.hex

data class Point(val x: String, val y: String)

@RunWith(JUnit4::class)
class CryptoTests {
    private val mapper = jacksonObjectMapper();
    
    fun testSign() {
        val private = Crypto.sm3("0x00".hex().bytes).hex()
        println(private)

        val pub = Crypto.sm2PkFromSk(private.bytes, true)
        val sig = Crypto.sm2Sign(1, private.bytes, "ffff".hex().bytes)

        println(sig.hex())

        val re = Crypto.sm2Verify(2, "ffff".hex().bytes, pub, sig)
        println(re)
    }


    fun testSchnorr() {
        val private = Crypto.schnorrGenSigner(0);
        println(private)
    }
}