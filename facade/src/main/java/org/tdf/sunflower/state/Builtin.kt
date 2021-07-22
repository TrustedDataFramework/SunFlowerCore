package org.tdf.sunflower.state

import org.tdf.common.util.ByteUtil
import org.tdf.common.util.HexBytes
import org.tdf.common.util.hex
import org.tdf.natives.Crypto
import org.tdf.sunflower.facade.RepositoryReader
import org.tdf.sunflower.vm.Backend
import org.tdf.sunflower.vm.CallContext
import org.tdf.sunflower.vm.CallData
import org.tdf.sunflower.vm.abi.Abi
import java.math.BigInteger

interface Builtin {
    val codeSize: Int get() = 1
    val address: HexBytes
    val genesisStorage: Map<HexBytes, HexBytes>
        get() = emptyMap()

    /**
     * call builtin contract by abi encoded parameters
     */
    fun call(rd: RepositoryReader, backend: Backend, ctx: CallContext, callData: CallData): ByteArray {
        return ByteUtil.EMPTY_BYTE_ARRAY
    }

    /**
     * call builtin contract by var args
     */
    fun call(
        rd: RepositoryReader,
        backend: Backend,
        ctx: CallContext,
        callData: CallData,
        method: String,
        vararg args: Any
    ): List<*>

    val abi: Abi

    /**
     * static call
     */
    fun view(rd: RepositoryReader, backend: Backend, method: String, vararg args: Any): List<*> {
        return emptyList<Any>()
    }
}

class CryptoContract: AbstractBuiltin(Constants.CRYPTO_CONTRACT_ADDR) {


    companion object {
        const val abiJson = """
[
  {
    "type": "constructor",
    "inputs": [],
    "outputs": [],
    "stateMutability": "nonpayable"
  },
  {
    "name": "sm3",
    "type": "function",
    "inputs": [
      {
        "name": "x",
        "type": "bytes"
      }
    ],
    "outputs": [
      {
        "type": "bytes32"
      }
    ],
    "stateMutability": "pure"
  },
  {
    "name": "sm2_pk_from_sk",
    "type": "function",
    "inputs": [
      {
        "name": "private_key",
        "type": "bytes32"
      },
      {
        "name": "compress",
        "type": "bool"
      }
    ],
    "outputs": [
      {
        "type": "bytes"
      }
    ],
    "stateMutability": "pure"
  },
  {
    "name": "sm2_verify",
    "type": "function",
    "inputs": [
      {
        "name": "seed",
        "type": "uint64"
      },
      {
        "name": "message",
        "type": "bytes"
      },
      {
        "name": "public_key",
        "type": "bytes"
      },
      {
        "name": "sig",
        "type": "bytes"
      }
    ],
    "outputs": [
      {
        "type": "bool"
      }
    ],
    "stateMutability": "pure"
  }
]            
        """
    }

    val ABI = Abi.fromJson(abiJson)
    override fun call(
        rd: RepositoryReader,
        backend: Backend,
        ctx: CallContext,
        callData: CallData,
        method: String,
        vararg args: Any
    ): List<*> {
        return when(method) {
            "sm3" -> listOf(Crypto.sm3(args[0] as ByteArray))
            "sm2_pk_from_sk" -> listOf(Crypto.sm2PkFromSk(args[0] as ByteArray, args[1] as Boolean))
            "sm2_verify" -> listOf(Crypto.sm2Verify((args[0] as BigInteger).longValueExact(), args[1] as ByteArray, args[2] as ByteArray, args[3] as ByteArray))
            else -> throw RuntimeException("invalid method $method")
        }
    }

    override val abi: Abi = ABI
}

class LoggingContract : AbstractBuiltin(Constants.LOGGING_CONTRACT_ADDR) {
    override fun call(
        rd: RepositoryReader,
        backend: Backend,
        ctx: CallContext,
        callData: CallData,
        method: String,
        vararg args: Any
    ): List<*> {
        val a = args[0]
        when(a) {
            is String -> println(a)
            is ByteArray -> println(a.hex())
        }
        return emptyList<Any>()
    }

    override val abi: Abi = ABI


    companion object {
        const val abiJson = """
[
    {
        "inputs": [
            {
                "name": "arg",
                "type": "string"
            }
        ],
        "name": "log",
        "outputs": [],
        "stateMutability": "pure",
        "type": "function"
    },
    {
        "inputs": [
            {
                "name": "arg",
                "type": "address"
            }
        ],
        "name": "log",
        "outputs": [],
        "stateMutability": "pure",
        "type": "function"
    }    
]
        """
        val ABI = Abi.fromJson(abiJson)
    }
}