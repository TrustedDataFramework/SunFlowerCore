package org.tdf.sunflower.state

import org.tdf.common.util.*

object Constants {
    val SIMPLE_BIOS_CONTRACT_ADDR = "0x0000000000000000000000000000000000000000".hex()

    @JvmField
    val POW_BIOS_ADDR = "0x0000000000000000000000000000000000000002".hex()

    val POA_AUTHENTICATION_ADDR = "0x0000000000000000000000000000000000000004".hex()
    val POS_CONTRACT_ADDR = "0x0000000000000000000000000000000000000005".hex()


    val LOGGING_CONTRACT_ADDR = "0x0000000000000000000000000000000000000001".hashed()

    // 3d70cb3f7732177a19f352814df621b506c237a4
    val CRYPTO_CONTRACT_ADDR = "crypto".hashed()

    private fun String.hashed(): HexBytes {
        return this.ascii().sha3().tail20().hex()
    }
}


