package org.tdf.sunflower.vm

import org.tdf.common.store.Store
import org.tdf.common.types.Uint256
import org.tdf.common.util.HexBytes
import org.tdf.sunflower.state.BuiltinContract
import java.io.Closeable

interface Backend: Closeable{
    fun subBalance(addr: HexBytes, amount: Uint256) {
        if (getBalance(addr) < amount) throw RuntimeException(
            String.format(
                "balance of account %s less than %s",
                addr,
                amount.value.toString(10)
            )
        )
        setBalance(addr, getBalance(addr) - amount)
    }

    fun addBalance(addr: HexBytes, amount: Uint256) {
        setBalance(addr, getBalance(addr) + amount)
    }

    fun getContractHash(address: HexBytes): HexBytes
    val height: Long
    val root: Backend
    val trieRoot: HexBytes
    val parentHash: HexBytes
    fun getBalance(address: HexBytes): Uint256

    // only used for bios contract
    var headerCreatedAt: Long?
    fun setBalance(address: HexBytes, balance: Uint256?)
    fun getNonce(address: HexBytes): Long
    fun setNonce(address: HexBytes, nonce: Long)
    fun getInitialGas(create: Boolean, data: ByteArray): Long {
        var gas = if (create) { GasConfig.TRANSACTION_CREATE_CONTRACT } else { GasConfig.TRANSACTION }
        val zero: Byte = 0
        var i = 0;
        while(i < data.size) {
            gas += if (data[i] == zero) { GasConfig.TX_ZERO_DATA } else { GasConfig.TX_NO_ZERO_DATA }
            i++
        }
        if(gas < 0)
            throw RuntimeException("gas overflow")
        return gas
    }
    val builtins: Map<HexBytes, BuiltinContract>
    val bios: Map<HexBytes, BuiltinContract>
    fun dbSet(address: HexBytes, key: HexBytes, value: HexBytes)

    // return empty byte array if key not found
    fun dbGet(address: HexBytes, key: HexBytes): HexBytes
    fun dbHas(address: HexBytes, key: HexBytes): Boolean

    fun dbRemove(address: HexBytes, key: HexBytes)
    fun getCode(address: HexBytes): HexBytes
    fun setCode(address: HexBytes, code: HexBytes)
    fun onEvent(address: HexBytes, topics: List<Uint256>, data: ByteArray)
    val isStatic: Boolean
    val parentBackend: Backend?
    fun createChild(): Backend

    // merge modifications, return the new state root
    fun merge(): HexBytes
    fun isContract(address: HexBytes): Boolean {
        val code = getCode(address)
        return code.size() > 0
    }

    fun getAsStore(address: HexBytes): Store<HexBytes, HexBytes> {
        return object : Store<HexBytes, HexBytes> {
            override operator fun get(bytes: HexBytes): HexBytes {
                return dbGet(address, bytes)
            }

            override fun put(bytes: HexBytes, bytes2: HexBytes) {
                dbSet(address, bytes, bytes2)
            }

            override fun remove(bytes: HexBytes) {
                dbRemove(address, bytes)
            }

            override fun flush() {
                throw RuntimeException("not implemented")
            }
        }
    }
}