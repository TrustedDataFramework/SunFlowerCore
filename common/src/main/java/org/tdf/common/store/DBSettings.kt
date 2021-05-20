package org.tdf.common.store

class DBSettings private constructor() {
    var maxOpenFiles = 0
        private set
    var maxThreads = 0
        private set
    
    fun withMaxOpenFiles(maxOpenFiles: Int): DBSettings {
        this.maxOpenFiles = maxOpenFiles
        return this
    }

    fun withMaxThreads(maxThreads: Int): DBSettings {
        this.maxThreads = maxThreads
        return this
    }

    companion object {
        @JvmField
        val DEFAULT = DBSettings()
            .withMaxThreads(1)
            .withMaxOpenFiles(32)

        @JvmStatic
        fun newInstance(): DBSettings {
            val settings = DBSettings()
            settings.maxOpenFiles = DEFAULT.maxOpenFiles
            settings.maxThreads = DEFAULT.maxThreads
            return settings
        }
    }
}