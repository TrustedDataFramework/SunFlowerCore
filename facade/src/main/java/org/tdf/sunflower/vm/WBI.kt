package org.tdf.sunflower.vm

import org.tdf.common.types.Uint256
import org.tdf.common.util.HexBytes
import org.tdf.lotusvm.ModuleInstance
import org.tdf.lotusvm.types.CustomSection
import org.tdf.lotusvm.Module
import org.tdf.sunflower.vm.abi.Abi
import org.tdf.sunflower.vm.abi.WbiType
import java.io.ByteArrayOutputStream
import java.math.BigInteger
import java.nio.charset.StandardCharsets

object WBI {
    const val ABI_SECTION_NAME = "__abi";
    const val INIT_SECTION_NAME = "__init"
    const val WBI_MALLOC = "__malloc"
    const val WBI_PEEK = "__peek"
    const val WBI_CHANGE_TYPE = "__change_t"

    @JvmStatic
    fun dropInit(code: ByteArray): ByteArray {
        Module.create(code).use {
            val out = ByteArrayOutputStream()
            // drop __init sections
            var now = 0
            for (section in it.customSections) {
                if (section.name == INIT_SECTION_NAME) {
                    out.write(code, now, section.offset - now)
                    now = section.limit
                }
            }
            out.write(code, now, code.size - now)
            return out.toByteArray()
        }
    }

    @JvmStatic
    fun extractInitData(m: Module): ByteArray {
        return m.customSections.stream().filter { x: CustomSection -> x.name == INIT_SECTION_NAME }.findFirst()
            .map { obj: CustomSection -> obj.data }.orElse(ByteArray(0))
    }

    // the __init section is dropped before inject
    @JvmStatic
    fun inject(create: Boolean, abi: Abi, i: ModuleInstance, input: HexBytes): InjectResult {
        var function: String? = null
        var entry: Abi.Entry? = null
        var encoded: HexBytes? = null

        // 2. for contract create, constructor is not necessarily
        if (create && abi.findConstructor() != null) {
            entry = abi.findConstructor()
            function = "init"
            encoded = input
        }

        // 3. for contract call, find function by signature
        if (!create) {
            val sig = input.slice(0, 4).bytes
            val f = abi.findFunction { x: Abi.Function -> x.encodeSignature().contentEquals(sig) }!!
            entry = f
            function = f.name
            // drop signature parts
            encoded = input.slice(4)
        }

        // params == null -> abi not found
        if (entry == null)
            return InjectResult(function ?: "", entry, LongArray(0), false)

        // malloc param types
        val inputs = Abi.Entry.Param.decodeList(entry.inputs, encoded!!.bytes)
        val ret = LongArray(entry.inputs.size)
        for (j in inputs.indices) {
            val p = entry.inputs[j]
            when (p.type.name) {
                "uint8", "uint16", "uint32", "uint64" -> {
                    ret[j] = (inputs[j] as BigInteger).longValueExact()
                }
                "uint", "uint256" -> {
                    val b = inputs[j] as BigInteger
                    ret[j] = malloc(i, Uint256.of(b)).toLong()
                }
                "string" -> {
                    val s = inputs[j] as String
                    ret[j] = malloc(i, s).toLong()
                }
                "address" -> {
                    val addr = inputs[j] as ByteArray
                    ret[j] = mallocAddress(i, HexBytes.fromBytes(addr)).toLong()
                }
                else -> {
                    if (p.type.name.endsWith("]") || p.type.name.endsWith(")")) {
                        throw RuntimeException("array or tuple is not supported yet")
                    }
                    if (p.type.name.startsWith("bytes")) {
                        val data = inputs[j] as ByteArray
                        ret[j] = mallocBytes(i, HexBytes.fromBytes(data)).toLong()
                    }
                }
            }
        }
        return InjectResult(function!!, entry, ret, true)
    }

    // String / U256 / HexBytes
    @JvmStatic
    fun peek(instance: ModuleInstance, offset: Int, type: Long): Any {
        val startAndLen = instance.execute(WBI_PEEK, offset.toLong(), type)[0]
        val start = (startAndLen ushr 32).toInt()
        val len = startAndLen.toInt()
        val bin = ByteArray(len)
        instance.memory.read(start, bin)
        when (type) {
            WbiType.STRING -> {
                return String(bin, StandardCharsets.UTF_8)
            }
            WbiType.UINT_256 -> {
                return Uint256.of(bin)
            }
            WbiType.BYTES, WbiType.ADDRESS -> {
                return HexBytes.fromBytes(bin)
            }
        }
        throw RuntimeException("unexpected")
    }

    private fun mallocInternal(instance: ModuleInstance, type: Long, bin: ByteArray): Int {
        val ptr = instance.execute(WBI_MALLOC, bin.size.toLong())[0]
        instance.memory.write(ptr.toInt(), bin)
        val p = instance.execute(WBI_CHANGE_TYPE, type, ptr, bin.size.toLong())[0]
        val r = p.toInt()
        if (r < 0) throw RuntimeException("malloc failed: pointer is negative")
        return r
    }

    fun malloc(instance: ModuleInstance, s: String): Int {
        val bin = s.toByteArray(StandardCharsets.UTF_8)
        return mallocInternal(instance, WbiType.STRING, bin)
    }

    @JvmStatic
    fun malloc(instance: ModuleInstance, s: Uint256): Int {
        val bin = s.noLeadZeroesData
        return mallocInternal(instance, WbiType.UINT_256, bin)
    }

    @JvmStatic
    fun mallocBytes(instance: ModuleInstance, bin: HexBytes): Int {
        return mallocInternal(instance, WbiType.BYTES, bin.bytes)
    }

    @JvmStatic
    fun mallocAddress(instance: ModuleInstance, address: HexBytes): Int {
        return mallocInternal(instance, WbiType.ADDRESS, address.bytes)
    }

    data class InjectResult(val name: String, val entry: Abi.Entry?, val pointers: LongArray, val executable: Boolean);
}