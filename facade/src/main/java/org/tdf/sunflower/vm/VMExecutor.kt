package org.tdf.sunflower.vm

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import org.slf4j.LoggerFactory
import org.tdf.common.types.Uint256
import org.tdf.common.util.ByteUtil
import org.tdf.common.util.HashUtil
import org.tdf.common.util.HexBytes
import org.tdf.evm.EvmCallData
import org.tdf.evm.EvmContext
import org.tdf.evm.Interpreter
import org.tdf.lotusvm.Module.Companion.create
import org.tdf.lotusvm.ModuleInstance
import org.tdf.lotusvm.runtime.ResourceFactory.createMemory
import org.tdf.lotusvm.runtime.ResourceFactory.createStack
import org.tdf.sunflower.facade.RepositoryReader
import org.tdf.sunflower.state.Address
import org.tdf.sunflower.types.LogInfo
import org.tdf.sunflower.types.VMResult
import org.tdf.sunflower.vm.ModuleValidator.validate
import org.tdf.sunflower.vm.WBI.InjectResult
import org.tdf.sunflower.vm.WBI.dropInit
import org.tdf.sunflower.vm.WBI.extractInitData
import org.tdf.sunflower.vm.WBI.inject
import org.tdf.sunflower.vm.WBI.peek
import org.tdf.sunflower.vm.abi.Abi
import org.tdf.sunflower.vm.abi.WbiType
import org.tdf.sunflower.vm.hosts.*
import java.io.PrintStream
import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

data class VMExecutor(
    var rd: RepositoryReader,
    var backend: Backend,
    var ctx: CallContext,
    var callData: CallData,
    var limit: Limit,
    var logs: MutableList<LogInfo>,
    var depth: Int = 0,
) {
    // gas limit hook


    constructor(rd: RepositoryReader, backend: Backend, ctx: CallContext, callData: CallData, gasLimit: Long) : this(
        rd,
        backend,
        ctx,
        callData,
        Limit(gasLimit),
        ArrayList<LogInfo>()
    )

    fun clone(): VMExecutor {
        if (depth + 1 == MAX_CALL_DEPTH) throw RuntimeException("vm call depth overflow")
        return VMExecutor(rd, backend, ctx, callData.clone(), limit, logs, depth + 1)
    }

    fun execute(): VMResult {
        // 1. increase sender nonce
        val n = backend.getNonce(ctx.origin)
        if (n != ctx.txNonce && callData.callType !== CallType.COINBASE)
            throw RuntimeException("invalid nonce")

        var contractAddress: HexBytes = Address.empty()
        if (!backend.staticCall && callData.callType === CallType.CALL) {
            backend.setNonce(ctx.origin, n + 1)
            // contract deploy nonce will increase in executeInternal
        }
        if (callData.callType === CallType.CREATE) {
            contractAddress = HashUtil.calcNewAddrHex(
                callData.caller.bytes,
                ctx.txNonce
            )
            callData = callData.copy(to = contractAddress)
        }

        // 2. set initial gas by payload size
        if (callData.callType !== CallType.COINBASE)
            limit.initialGas = backend.getInitialGas(callData.callType === CallType.CREATE, callData.data.bytes)
        val result = executeInternal()

        // 3. calculate fee and
        val fee = Uint256.of(limit.gas).times(ctx.gasPrice)
        backend.subBalance(ctx.origin, fee)

        return VMResult(
            limit.gas,
            contractAddress,
            result.hex(),
            logs,
            fee
        )
    }

    private fun isWasm(bytes: ByteArray): Boolean {
        return bytes.size >= WASM_MAGIC.size
                &&
                Arrays.equals(bytes, 0, WASM_MAGIC.size, WASM_MAGIC, 0, WASM_MAGIC.size)
    }

    private fun ByteArray.hex(): HexBytes {
        return HexBytes.fromBytes(this)
    }

    fun executeInternal(): ByteArray {
        return when (callData.callType) {
            CallType.COINBASE -> {
                backend.addBalance(callData.to, callData.value)
                for (bios in backend.bios.values) {
                    return bios.call(rd, backend, ctx, callData)
                }
                ByteUtil.EMPTY_BYTE_ARRAY
            }
            CallType.DELEGATE, CallType.CALL, CallType.CREATE -> {

                // is prebuilt
                if (backend.builtins.containsKey(callData.to)) {
                    backend.addBalance(callData.to, callData.value)
                    backend.subBalance(callData.caller, callData.value)
                    val updater = backend.builtins[callData.to]!!
                    return updater.call(rd, backend, ctx, callData)
                }
                var code: ByteArray
                // contract constructor/call arguments
                var data: ByteArray
                // if call context is evm, else web assembly
                val isWasm: Boolean
                val receiver = callData.to
                val create = callData.callType === CallType.CREATE
                when (callData.callType) {
                    CallType.CREATE -> {
                        isWasm = isWasm(callData.data.bytes)

                        // increase sender nonce
                        val n = backend.getNonce(callData.caller)
                        if (isWasm) {
                            create(callData.data.bytes).use { tmpModule ->
                                // validate module
                                validate(tmpModule, false)
                                code = dropInit(callData.data.bytes)
                                data = extractInitData(tmpModule)
                                backend.setCode(receiver, code.hex())
                            }
                        } else {
                            code = callData.data.bytes
                            data = ByteUtil.EMPTY_BYTE_ARRAY
                        }

                        // increase nonce here to avoid conflicts
                        backend.setNonce(callData.caller, n + 1)
                    }
                    CallType.CALL -> {
                        val hash = backend.getContractHash(receiver)
                        // this is a transfer transaction
                        code = if (hash == HashUtil.EMPTY_DATA_HASH_HEX) {
                            HexBytes.EMPTY_BYTES
                        } else {
                            CACHE[hash, { backend.getCode(receiver).bytes }]
                        }
                        data = callData.data.bytes
                        isWasm = isWasm(code)
                    }
                    CallType.DELEGATE -> {
                        val hash = backend.getContractHash(callData.delegateAddr)
                        // this is a transfer transaction
                        code = if (hash == HashUtil.EMPTY_DATA_HASH_HEX) {
                            HexBytes.EMPTY_BYTES
                        } else {
                            CACHE[hash, { backend.getCode(callData.delegateAddr).bytes }]
                        }
                        data = callData.data.bytes
                        isWasm = isWasm(code)
                    }
                    else -> throw UnsupportedOperationException()
                }
                // call a non-contract account
                if (code.isEmpty() && !callData.data.isEmpty) throw RuntimeException("call receiver not a contract")
                backend.addBalance(receiver, callData.value)
                backend.subBalance(callData.caller, callData.value)
                if (code.isEmpty()) return ByteUtil.EMPTY_BYTE_ARRAY
                if (isWasm) executeWasm(create, code, data) else executeEvm(create, code, data)
            }
        }
    }

    private fun executeEvm(create: Boolean, code: ByteArray, input: ByteArray): ByteArray {
        val evmCallData = EvmCallData(
            callData.caller.bytes,
            callData.to.bytes,
            callData.value.value,
            input,
            code
        )
        val ctx = EvmContext(
            origin = ctx.origin.bytes,
            number = backend.height,
            chainId = ctx.chainId
        )
        val host = EvmHostImpl(this, rd)
        val interpreter =
            Interpreter(host, ctx, evmCallData, printStream, limit, EVM_MAX_STACK_SIZE, EVM_MAX_MEMORY_SIZE)
        val ret = interpreter.execute()
        if (create) {
            backend.setCode(callData.to, ret.hex())
        }
        return if (create) ByteUtil.EMPTY_BYTE_ARRAY else ret
    }

    private fun executeWasm(create: Boolean, code: ByteArray, data: ByteArray): ByteArray {
        // transfer to a wasm contract account
        if (callData.data.isEmpty) {
            return ByteUtil.EMPTY_BYTE_ARRAY
        }
        val dbFunctions = DBFunctions(backend, callData.to)

        val hosts = Hosts(
            context = ContextHost(callData),
            reflect = Reflect(this),
            transfer = Transfer(backend, callData.to),
            db = dbFunctions,
            u256 = U256Host()
        )


        createStack(MAX_STACK_SIZE, MAX_FRAMES, MAX_LABELS).use { stack ->
            createMemory().use { mem ->
                create(code).use { module ->
                    val instance = ModuleInstance.builder()
                        .stackAllocator(stack)
                        .module(module)
                        .memory(mem)
                        .hooks(setOf(limit))
                        .hostFunctions(hosts.all)
                        .build()

                    val abi: Abi = module.customSections
                        .firstOrNull { it.name == WBI.ABI_SECTION_NAME }
                        ?.let { Abi.fromJson(String(it.data, StandardCharsets.UTF_8)) }
                        ?: throw RuntimeException("abi not found in creation code")

                    val rets: LongArray
                    val r: InjectResult = inject(create, abi, instance, data.hex())

                    // payable check
                    val payable = r.entry != null && r.entry.isPayable

                    if (!payable && !callData.value.isZero)
                        throw RuntimeException("function " + r.name + " is not payable")

                    if (!r.executable) return ByteUtil.EMPTY_BYTE_ARRAY


                    rets = instance.execute(r.name, *r.pointers)

                    val results: MutableList<Any> = mutableListOf()

                    val outputs: List<Abi.Entry.Param> = if (create) {
                        abi.findConstructor()?.outputs
                            ?: emptyList()
                    } else {
                        abi.findFunction { it.name.equals(r.name) }.outputs!!
                    }


                    // extract result
                    for (i in rets.indices) {
                        val type = outputs[i].type
                        when (type.name) {
                            "uint8", "uint16", "uint32", "uint64" -> {
                                results.add(BigInteger.valueOf(rets[i]))
                            }
                            "uint", "uint256" -> {
                                val u = peek(instance, rets[i].toInt(), WbiType.UINT_256) as Uint256
                                results.add(u.value)
                            }
                            "string" -> {
                                val s = peek(instance, rets[i].toInt(), WbiType.STRING) as String
                                results.add(s)
                            }
                            "address" -> {
                                val addr = peek(instance, rets[i].toInt(), WbiType.ADDRESS) as HexBytes
                                results.add(addr.bytes)
                            }
                            else -> {
                                if (type.name.endsWith("]") || type.name.endsWith(")")) {
                                    throw RuntimeException("array or tuple is not supported")
                                }
                                if (type.name.startsWith("bytes")) {
                                    val bytes = peek(instance, rets[i].toInt(), WbiType.BYTES) as HexBytes
                                    results.add(bytes.bytes)
                                }
                            }
                        }
                    }
                    return Abi.Entry.Param.encodeList(outputs, *results.toTypedArray())
                }
            }
        }
    }

    companion object {
        const val MAX_FRAMES = 16384
        const val MAX_STACK_SIZE = MAX_FRAMES * 4
        const val MAX_LABELS = MAX_FRAMES * 4
        const val MAX_CALL_DEPTH = 8
        const val EVM_MAX_STACK_SIZE = 1024

        // 16 mb
        const val EVM_MAX_MEMORY_SIZE = 1024 * 1024 * 16

        val CACHE: Cache<HexBytes, ByteArray> = CacheBuilder
            .newBuilder()
            .weigher { k: Any, v: Any -> (v as ByteArray).size + (k as HexBytes).size() }
            .maximumWeight(1024L * 1024L * 8L) // 8mb cache for contracts
            .build()

        private val COUNTER = AtomicInteger()
        private val WASM_MAGIC = byteArrayOf(0x00, 0x61, 0x73, 0x6d)
        private val log = LoggerFactory.getLogger("vm")
        private var outDirectory = ""

        fun enableDebug(outDirectory: String) {
           this.outDirectory = outDirectory
        }

        private val printStream: PrintStream?
            get() {
                if (outDirectory.isEmpty())
                    return null
                val filename = String.format("%04d.log", COUNTER.incrementAndGet())
                val path = Paths.get(outDirectory, filename)
                log.info("write vm debug log to file {}", path)
                val os = Files.newOutputStream(
                    path,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE
                )
                return PrintStream(os)
            }
    }
}