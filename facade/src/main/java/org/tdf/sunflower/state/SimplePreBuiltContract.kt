package org.tdf.sunflower.state

import org.tdf.common.util.HexBytes
import org.tdf.sunflower.vm.abi.Abi

class SimplePreBuiltContract : BuiltinContract {
    override val address: HexBytes
        get() = Constants.SIMPLE_BIOS_CONTRACT_ADDR
    override val abi: Abi
        get() = Abi.fromJson("[]")
}