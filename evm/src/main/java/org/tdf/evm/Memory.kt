package org.tdf.evm

import org.tdf.evm.SlotUtils.MAX_BYTE_ARRAY_SIZE

interface Memory {
    operator fun get(idx: Int): Byte {
        return data[idx]
    }

    operator fun set(idx: Int, v: Byte) {
        data[idx] = v
    }

    fun resize(size: Int)

    val data: ByteArray
    val limit: Int
    val size: Int

    fun write(off: Int, buf: ByteArray, bufOff: Int = 0, bufSize: Int = buf.size) {
        if (off < 0 || off + bufSize > size)
            throw RuntimeException("memory access overflow")

        for (i in 0 until bufSize) {
            this[off + i] = buf[bufOff + i]
        }
    }

    fun read(off: Int, buf: ByteArray, bufOff: Int = 0, bufSize: Int = buf.size) {
        if (off < 0 || off + bufSize > size)
            throw RuntimeException("memory access overflow")
        for (i in 0 until bufSize) {
            buf[bufOff + i] = this[off + i]
        }
    }
}

class MemoryImpl(override val limit: Int = Int.MAX_VALUE) : Memory {
    override fun resize(size: Int) {
        if(size < 0 || size > limit)
            throw RuntimeException("memory size exceeds limit")
        if(size > data.size) {
            val tmp = ByteArray(size)
            System.arraycopy(data, 0, tmp, 0, data.size)
            this.data = tmp
        }
    }

    override var data: ByteArray = ByteArray(MAX_BYTE_ARRAY_SIZE * 32)
        private set

    override var size: Int = 0
        private set
}