package org.tdf.common.trie

import org.tdf.common.store.Store
import org.tdf.common.util.HexBytes

class ReadOnlyTrie<K, V> (private val delegate: AbstractTrie<K, V>) : Trie<K, V> by delegate {
    override fun revert(rootHash: HexBytes, store: Store<ByteArray, ByteArray>): Trie<K, V> {
        throw UnsupportedOperationException()
    }

    override fun revert(rootHash: HexBytes): Trie<K, V> {
        throw UnsupportedOperationException()
    }

    override fun revert(): Trie<K, V> {
        throw UnsupportedOperationException()
    }

    override fun commit(): HexBytes {
        throw UnsupportedOperationException()
    }

    override fun set(k: K, v: V) {
        throw UnsupportedOperationException()
    }

    override fun remove(k: K) {
        throw UnsupportedOperationException()
    }

    override fun flush() {
        throw UnsupportedOperationException()
    }

    companion object {
        @JvmStatic
        fun <K, V> of(trie: Trie<K, V>): Trie<K, V> {
            if (trie is ReadOnlyTrie<*, *>) return trie
            if (trie.isDirty) throw UnsupportedOperationException()
            return ReadOnlyTrie(trie as AbstractTrie<K, V>)
        }
    }
}