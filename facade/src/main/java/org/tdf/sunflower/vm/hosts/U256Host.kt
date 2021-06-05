package org.tdf.sunflower.vm.hosts

import com.github.salpadding.evm.Slot
import com.github.salpadding.evm.SlotImpl
import org.tdf.lotusvm.runtime.HostFunction
import org.tdf.lotusvm.types.FunctionType
import org.tdf.lotusvm.types.ValueType
import org.tdf.sunflower.vm.WBI
import org.tdf.sunflower.vm.abi.WbiType

class U256Host : HostFunction("_u256", FUNCTION_TYPE) {
    private val slot0: Slot = SlotImpl()
    private val slot1: Slot = SlotImpl()
    private val slot2: Slot = SlotImpl()
    private val slot3: Slot = SlotImpl()

    private val tmpData = ByteArray(32)

    internal enum class U256OP {
        ADD, SUB, MUL, DIV, MOD
    }

    private fun clearTmp() {
        for(i in 0 until tmpData.size)
            tmpData[i] = 0
    }

    private fun loadSlot(offset: Int, slot: Slot) {
        val startAndLen = instance.execute(WBI.WBI_PEEK, offset.toLong(), WbiType.UINT_256)[0]
        val start = (startAndLen ushr 32).toInt()
        val len = startAndLen.toInt()

        clearTmp()
        for(i in 0 until len) {
            tmpData[tmpData.size - len + i] = instance.memory.load8(start + i)
        }

        slot.decodeBE(tmpData, 0)
    }

    private fun putSlot(slot: Slot): Long {
        clearTmp()
        slot.encodeBE(tmpData, 0)
        var firstNoZero = -1

        for(i in 0 until tmpData.size) {
            if(tmpData[i] != (0).toByte()) {
                firstNoZero = i
                break
            }
        }

        val size = if (firstNoZero < 0) { 0 } else { tmpData.size - firstNoZero }
        val ptr = (instance.execute(WBI.WBI_MALLOC, size.toLong())[0]).toInt()
        if (ptr < 0) throw RuntimeException("malloc failed: pointer is negative")

        for(i in 0 until size) {
            instance.memory.storeI8(ptr + i, tmpData[firstNoZero + i])
        }

        val p = instance.execute(WBI.WBI_CHANGE_TYPE, WbiType.UINT_256, ptr.toLong(), size.toLong())[0]
        val r = p.toInt()
        if (r < 0) throw RuntimeException("malloc failed: pointer is negative")
        return r.toLong()
    }

    override fun execute(args: LongArray): Long {
        val i = args[0].toInt()
        val op = U256OP.values()[i]
        loadSlot(args[1].toInt(), slot0)
        loadSlot(args[2].toInt(), slot1)

        when (op) {
            U256OP.ADD -> {
                slot0.add(slot1)
                return putSlot(slot0)
            }
            U256OP.SUB -> {
                slot0.sub(slot1)
                return putSlot(slot0)
            }
            U256OP.MUL -> {
                slot2.initZeros()
                slot0.mul(slot1, slot2)
                return putSlot(slot2)
            }
            U256OP.DIV -> {
                slot2.initZeros()
                slot3.initZeros()
                slot0.divMod(slot1, slot2, slot3)
                return putSlot(slot2)
            }
            U256OP.MOD -> {
                slot2.initZeros()
                slot3.initZeros()
                slot0.divMod(slot1, slot2, slot3)
                return putSlot(slot0)
            }
        }
    }

    companion object {
        val FUNCTION_TYPE = FunctionType( // offset, length, offset
            listOf(ValueType.I64, ValueType.I64, ValueType.I64), listOf(ValueType.I64)
        )
    }
}