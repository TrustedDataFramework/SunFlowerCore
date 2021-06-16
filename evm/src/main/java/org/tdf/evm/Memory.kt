package org.tdf.evm

interface Memory {
    operator fun get(idx: Int): Byte {
        return data[idx]
    }

    operator fun set(idx: Int, v: Byte) {
        data[idx] = v
    }

    val data: ByteArray
    val limit: Int
    val size: Int

    fun write(off: Int, buf: ByteArray, bufOff: Int = 0, bufSize: Int = buf.size) {
        if (off + bufSize > size)
            throw RuntimeException("memory access overflow")

        for (i in 0 until bufSize) {
            this[off + i] = buf[bufOff + i]
        }
    }

    fun read(off: Int, buf: ByteArray, bufOff: Int = 0, bufSize: Int = buf.size) {
        if (off + bufSize > size)
            throw RuntimeException("memory access overflow")
        for (i in 0 until bufSize) {
            buf[bufOff + i] = this[off + i]
        }
    }
}