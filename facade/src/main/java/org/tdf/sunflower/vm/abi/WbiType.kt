package org.tdf.sunflower.vm.abi

object WbiType {
    const val UINT_256 = -0x13ec292f // keccak(uint256)
    const val ADDRESS = 0x421683f8 // keccak(address)
    const val STRING = -0x6803b9d9 // keccak(string)
    const val BYTES = -0x469c164c // keccak(bytes)
}