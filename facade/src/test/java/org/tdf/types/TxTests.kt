package org.tdf.types

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.tdf.common.util.decode
import org.tdf.sunflower.types.Transaction

@RunWith(JUnit4::class)
class TxTests {

    @Test
    fun test0() {
        val t = Transaction()

        assert(t == t.encoded.decode(Transaction::class.java))
    }
}