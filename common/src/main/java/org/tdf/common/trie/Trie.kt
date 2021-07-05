package org.tdf.common.trie

import org.tdf.common.serialize.Codec
import org.tdf.common.serialize.Codec.Companion.identity
import org.tdf.common.util.HexBytes
import org.tdf.common.util.HashUtil
import java.util.AbstractMap.SimpleImmutableEntry
import org.tdf.common.store.ByteArrayMapStore
import org.tdf.common.store.Store
import java.util.function.BiFunction
import java.util.function.Function
import java.util.stream.Stream

interface Trie<K, V> : Store<K, V> {
    val store: Store<ByteArray, ByteArray>

    /**
     * rollback to a previous trie
     *
     * @param rootHash previous trie's root hash
     * @param store    the underlying storage of trie
     * @return trie with root hash
     * @throws RuntimeException if the root hash not found in the store or rollback failed
     */
    fun revert(rootHash: HexBytes, store: Store<ByteArray, ByteArray>): Trie<K, V>

    /**
     * rollback to a previous trie
     *
     * @param rootHash previous trie's root hash
     * @return trie with root hash
     * @throws RuntimeException if the root hash not found in the store or roll back failed
     */
    fun revert(rootHash: HexBytes): Trie<K, V>

    /**
     * rollback to an empty trie
     *
     * @return an empty trie
     */
    fun revert(): Trie<K, V>

    /**
     * commit modifications and build a new trie
     *
     * @return root hash of new trie
     */
    fun commit(): HexBytes

    /**
     * dump keys this trie
     *
     * @return minimal key value pairs to store this trie
     * @throws RuntimeException if the trie is both non-null and dirty
     */
    fun dumpKeys(): Set<HexBytes>

    /**
     * dump this trie
     *
     * @return minimal key value pairs to store this trie
     * @throws RuntimeException if the trie is both non-null and dirty
     */
    fun dump(): Map<HexBytes, HexBytes>

    /**
     * get root hash of a non-dirty tree, return null hash if the trie is null
     *
     * @return trie's root hash
     * @throws RuntimeException if this trie is dirty
     */
    val rootHash: HexBytes

    /**
     * get root hash of an empty trie
     *
     * @return root hash of an empty trie
     */
    val nullHash: HexBytes
        get() = HashUtil.EMPTY_TRIE_HASH_HEX

    /**
     * trie is both non-null and has uncommitted modifications
     *
     * @return true when trie is both non-null and dirty
     */
    val isDirty: Boolean

    val size: Int get() {
        val count = IntArray(1)
        traverseValue { v: V ->
            count[0]++
            true
        }
        return count[0]
    }

    fun stream(): Stream<Map.Entry<K, V>> {
        val builder: Stream.Builder<Map.Entry<K, V>> = Stream.builder()
        traverse { k: K, v: V ->
            builder.accept(SimpleImmutableEntry(k, v))
            true
        }
        return builder.build()
    }

    fun traverse(traverser: BiFunction<in K, in V, Boolean>)
    fun traverseValue(traverser: Function<in V, Boolean>)
}