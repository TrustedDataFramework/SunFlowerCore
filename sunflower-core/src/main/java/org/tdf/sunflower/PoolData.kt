package org.tdf.sunflower

import java.math.BigInteger

class PoolData (
    var chainId: Long = 0,
    var address: ByteArray,
    var poolType: Long = 0,
    var f: BigInteger? = null,
    var fm: BigInteger? = null,
    var r: BigInteger? = null,
    var rm: BigInteger? = null,
    var debt: BigInteger? = null,
    var price: BigInteger? = null,
    var decimals: Long = 0,
){}