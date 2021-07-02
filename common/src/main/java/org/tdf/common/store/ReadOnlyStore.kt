package org.tdf.common.store

import org.tdf.common.trie.ReadOnlyTrie

class ReadOnlyStore<K, V> private constructor(private val delegate: Store<K, V>) : Store<K, V> by delegate {
    override fun set(k: K, v: V) {
        throw UnsupportedOperationException(READ_ONLY_TIP)
    }

    override fun remove(k: K) {
        throw UnsupportedOperationException(READ_ONLY_TIP)
    }

    override fun flush() {
        throw UnsupportedOperationException(READ_ONLY_TIP)
    }

    companion object {
        private const val READ_ONLY_TIP = "the store is read only"
        fun <K, V> of(delegate: Store<K, V>): Store<K, V> {
            return if (delegate is ReadOnlyStore<*, *> || delegate is ReadOnlyTrie<*, *>) delegate else ReadOnlyStore(
                delegate
            )
        }
    }
}