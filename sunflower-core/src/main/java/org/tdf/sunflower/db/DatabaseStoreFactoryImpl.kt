package org.tdf.sunflower.db

import org.fusesource.leveldbjni.JniDBFactory
import org.iq80.leveldb.impl.Iq80DBFactory
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.tdf.common.store.*
import org.tdf.sunflower.DatabaseConfig
import org.tdf.sunflower.facade.DatabaseStoreFactory

@Component
class DatabaseStoreFactoryImpl(config: DatabaseConfig) : DatabaseStoreFactory {
    private val created: MutableSet<String> = mutableSetOf()
    private val config: DatabaseConfig
    private var base: DatabaseStore
    override val directory: String
        get() = config.directory

    override fun create(prefix: Char): Store<ByteArray, ByteArray> {
        val str = String(charArrayOf(prefix))
        if (created.contains(str))
            throw RuntimeException("this prefix had been used")
        created.add(str)
        return BasePrefixStore(base, byteArrayOf(prefix.code.toByte()))
    }

    override val name: String
        get() = config.name

    override fun cleanup() {}

    companion object {
        private val log = LoggerFactory.getLogger("db")
    }

    init {
        this.config = config
        when (config.name.trim { it <= ' ' }.lowercase()) {
            "leveldb-jni", "leveldb" -> base = LevelDb(JniDBFactory.factory, config.directory)
            "memory" -> base = MemoryDatabaseStore()
            "leveldb-iq80" -> base = LevelDb(Iq80DBFactory.factory, config.directory)
            else -> {
                base = LevelDb(JniDBFactory.factory, config.directory)
                log.warn("Data source is not supported, default is leveldb")
            }
        }
        base.init(
            DBSettings(
                config.maxOpenFiles, Math.max(1, Runtime.getRuntime().availableProcessors() / 2)
            )
        )
        if (config.reset) {
            base.clear()
        }
    }
}