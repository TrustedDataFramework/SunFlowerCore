package org.tdf.common.store


class MemoryDatabaseStore : ByteArrayMapStore<ByteArray>(), DatabaseStore {
    override fun init(settings: DBSettings) {}
    override val isAlive: Boolean get() {
        return true
    }

    override fun close() {}
    override fun clear() {
        cache.clear()
    }
}