package org.tdf.sunflower.state

import org.tdf.common.crypto.ECDSASignature
import org.tdf.common.crypto.ECKey
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

class CryptoContract : AbstractBuiltin(Constants.CRYPTO_CONTRACT_ADDR) {


    companion object {
        const val abiJson = """
[
	{
		"inputs": [
			{
				"internalType": "uint64",
				"name": "seed",
				"type": "uint64"
			},
			{
				"internalType": "bytes",
				"name": "msg",
				"type": "bytes"
			},
			{
				"internalType": "bytes32[]",
				"name": "decoys",
				"type": "bytes32[]"
			},
			{
				"internalType": "bytes32",
				"name": "challenge",
				"type": "bytes32"
			},
			{
				"internalType": "bytes32[]",
				"name": "responses",
				"type": "bytes32[]"
			},
			{
				"internalType": "bytes32[]",
				"name": "key_imgs",
				"type": "bytes32[]"
			}
		],
		"name": "mlsag_verify",
		"outputs": [
			{
				"internalType": "bool",
				"name": "",
				"type": "bool"
			}
		],
		"stateMutability": "pure",
		"type": "function"
	},
	{
		"inputs": [
			{
				"internalType": "uint64",
				"name": "seed",
				"type": "uint64"
			}
		],
		"name": "schnorr_gen_signer",
		"outputs": [
			{
				"internalType": "string",
				"name": "",
				"type": "string"
			}
		],
		"stateMutability": "pure",
		"type": "function"
	},
	{
		"inputs": [
			{
				"internalType": "uint256",
				"name": "index",
				"type": "uint256"
			},
			{
				"internalType": "bytes32",
				"name": "sk",
				"type": "bytes32"
			},
			{
				"internalType": "bytes",
				"name": "msg",
				"type": "bytes"
			},
			{
				"internalType": "string",
				"name": "pubs",
				"type": "string"
			},
			{
				"internalType": "string",
				"name": "r1s",
				"type": "string"
			}
		],
		"name": "schnorr_round_1",
		"outputs": [
			{
				"internalType": "string",
				"name": "",
				"type": "string"
			}
		],
		"stateMutability": "pure",
		"type": "function"
	},
	{
		"inputs": [
			{
				"internalType": "bytes32",
				"name": "prime",
				"type": "bytes32"
			},
			{
				"internalType": "string",
				"name": "r",
				"type": "string"
			},
			{
				"internalType": "bytes32[]",
				"name": "rs2",
				"type": "bytes32[]"
			}
		],
		"name": "schnorr_round_2",
		"outputs": [
			{
				"internalType": "bytes32",
				"name": "",
				"type": "bytes32"
			}
		],
		"stateMutability": "pure",
		"type": "function"
	},
	{
		"inputs": [
			{
				"internalType": "bytes32",
				"name": "sig",
				"type": "bytes32"
			},
			{
				"internalType": "string",
				"name": "r",
				"type": "string"
			},
			{
				"internalType": "bytes32",
				"name": "c",
				"type": "bytes32"
			},
			{
				"internalType": "string",
				"name": "x_tlide",
				"type": "string"
			}
		],
		"name": "schnorr_verify",
		"outputs": [
			{
				"internalType": "bytes32",
				"name": "",
				"type": "bytes32"
			}
		],
		"stateMutability": "pure",
		"type": "function"
	},
	{
		"inputs": [
			{
				"internalType": "bytes32",
				"name": "private_key",
				"type": "bytes32"
			},
			{
				"internalType": "bool",
				"name": "compress",
				"type": "bool"
			}
		],
		"name": "sm2_pk_from_sk",
		"outputs": [
			{
				"internalType": "bytes",
				"name": "",
				"type": "bytes"
			}
		],
		"stateMutability": "pure",
		"type": "function"
	},
	{
		"inputs": [
			{
				"internalType": "uint64",
				"name": "seed",
				"type": "uint64"
			},
			{
				"internalType": "bytes",
				"name": "message",
				"type": "bytes"
			},
			{
				"internalType": "bytes",
				"name": "public_key",
				"type": "bytes"
			},
			{
				"internalType": "bytes",
				"name": "sig",
				"type": "bytes"
			}
		],
		"name": "sm2_verify",
		"outputs": [
			{
				"internalType": "bool",
				"name": "",
				"type": "bool"
			}
		],
		"stateMutability": "pure",
		"type": "function"
	},
	{
		"inputs": [
			{
				"internalType": "bytes",
				"name": "x",
				"type": "bytes"
			}
		],
		"name": "sm3",
		"outputs": [
			{
				"internalType": "bytes32",
				"name": "",
				"type": "bytes32"
			}
		],
		"stateMutability": "pure",
		"type": "function"
	}
]            
     """
    }

    private fun Any.long() = (this as BigInteger).longValueExact()
    private fun Any.bytes() = this as ByteArray
    private fun Any.str() = this as String

    private fun Any.bytesArr(): Array<ByteArray> {
        val objs = this as Array<java.lang.Object>
        val ret = arrayOfNulls<ByteArray>(objs.size)
        System.arraycopy(objs, 0, ret, 0, objs.size)
        return ret.requireNoNulls()
    }

    private fun Any.bool() = this as Boolean

    val ABI = Abi.fromJson(abiJson)
    override fun call(
        rd: RepositoryReader,
        backend: Backend,
        ctx: CallContext,
        callData: CallData,
        method: String,
        vararg args: Any
    ): List<*> {
        return when (method) {
            "sm3" -> listOf(Crypto.sm3(args[0].bytes()))
            "sm2_pk_from_sk" -> listOf(Crypto.sm2PkFromSk(args[0].bytes(), args[1].bool()))
            "sm2_verify" -> listOf(Crypto.sm2Verify(args[0].long(), args[1].bytes(), args[2].bytes(), args[3].bytes()))
            "mlsag_verify" -> listOf(
                Crypto.mlsagVerify(
                    args[0].long(),
                    args[1].bytes(),
                    args[2].bytesArr(),
                    args[3].bytes(),
                    args[4].bytesArr(),
                    args[5].bytesArr()
                )
            )
            "schnorr_gen_signer" -> listOf(Crypto.schnorrGenSigner(0))
            "schnorr_round_1" -> listOf(
                Crypto.schnorrRound1(
                    args[0].long().toInt(),
                    args[1].bytes(),
                    args[2].bytes(),
                    args[3].str(),
                    args[4].str()
                )
            )
            "schnorr_round_2" -> listOf(
                Crypto.schnorrRound2(
                    args[0].bytes(),
                    args[1].str(),
                    args[2].bytesArr(),
                )
            )
            "schnorr_verify" -> listOf(
                Crypto.schnorrVerify(
                    args[0].bytes(),
                    args[1].str(),
                    args[2].bytes(),
                    args[3].str()
                )
            )
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
        when (a) {
            is String -> println(a)
            is ByteArray -> println(a.hex())
            is BigInteger -> println(a)
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
    },
    {
        "inputs": [
            {
                "name": "arg",
                "type": "uint256"
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