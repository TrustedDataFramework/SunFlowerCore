package org.tdf.common.trie

import org.tdf.common.util.HexBytes
import com.github.salpadding.rlpstream.Rlp
import org.tdf.common.serialize.Codec
import org.tdf.common.store.ReadOnlyStore
import org.tdf.common.store.Store
import java.util.function.BiFunction

// enhanced radix tree
class TrieImpl<K, V> private constructor(
    override var store: Store<ByteArray, ByteArray>,
    override var kCodec: Codec<K>,
    override var vCodec: Codec<V>,
    private var root: Node?
) : AbstractTrie<K, V>() {

    override fun getFromBytes(key: ByteArray?): V? {
        require(!(key == null || key.isEmpty())) { "key cannot be null" }
        if (root == null) return null
        val v = root!![TrieKey.fromNormal(key)]
        return if (v == null || v.isEmpty()) null else vCodec.decoder.apply(v)
    }

    override fun putBytes(key: ByteArray?, value: ByteArray?) {
        require(!(key == null || key.isEmpty())) { "key cannot be null" }
        if (value == null || value.isEmpty()) {
            removeBytes(key)
            return
        }
        if (root == null) {
            root = Node.newLeaf(TrieKey.fromNormal(key), value)
            return
        }
        root!!.insert(TrieKey.fromNormal(key), value, store)
    }

    override fun removeBytes(key: ByteArray?) {
        require(!(key == null || key.isEmpty())) { "key cannot be null" }
        if (root == null) return
        root = root!!.delete(TrieKey.fromNormal(key), store)
    }

    fun clear() {
        root = null
    }

    override fun commit(): HexBytes {
        if (root == null) return nullHash
        if (!root!!.isDirty) return HexBytes.fromBytes(root!!.hash)
        val hash = Rlp.decodeBytes(this.root!!.commit(store, true))
        if (root!!.isDirty || root!!.hash == null)
            throw RuntimeException("unexpected error: still dirty after commit")
        return HexBytes.fromBytes(hash)
    }

    override fun flush() {
        store.flush()
    }

    override fun revert(rootHash: HexBytes, store: Store<ByteArray, ByteArray>): TrieImpl<K, V> {
        if (rootHash == nullHash) return TrieImpl(
            store,
            kCodec, vCodec, null
        )
        val v = store[rootHash.bytes]
        if (v == null || v.size == 0) throw RuntimeException("rollback failed, root hash not exists")
        return TrieImpl(
            store, kCodec, vCodec,
            Node.fromRootHash(rootHash.bytes, ReadOnlyStore.of(store))
        )
    }

    private fun traverseTrie(action: ScannerAction) {
        if (root == null) return
        root!!.traverse(TrieKey.EMPTY, action)
    }

    override fun dumpKeys(): Set<HexBytes> {
        if (isDirty) throw UnsupportedOperationException()
        val dump = DumpKeys()
        traverseTrie(dump)
        return dump.keys
    }

    override fun dump(): Map<HexBytes, HexBytes> {
        if (isDirty) throw UnsupportedOperationException()
        val dump = Dump()
        traverseTrie(dump)
        return dump.pairs
    }

    @get:Throws(RuntimeException::class)
    override val rootHash: HexBytes
        get() {
            if (root == null) return nullHash
            if (root!!.isDirty || root!!.hash == null) throw RuntimeException("the trie is dirty or root hash is null")
            return HexBytes.fromBytes(root!!.hash)
        }
    override val isDirty: Boolean
        get() = root != null && root!!.isDirty

    override fun revert(rootHash: HexBytes): TrieImpl<K, V> {
        return revert(rootHash, store)
    }

    override fun revert(): TrieImpl<K, V> {
        return TrieImpl(
            store, kCodec, vCodec,
            null
        )
    }

    override fun traverseInternal(traverser: BiFunction<ByteArray, ByteArray, Boolean>) {
        traverseTrie { k: TrieKey, n: Node ->
            if (n.type != Node.Type.EXTENSION && n.value != null) {
                return@traverseTrie traverser.apply(k.toNormal(), n.value)
            }
            true
        }
    }

    companion object {
        @JvmStatic
        fun <K, V> newInstance(
            store: Store<ByteArray, ByteArray>,
            keyCodec: Codec<K>,
            valueCodec: Codec<V>
        ): TrieImpl<K, V> {
            return TrieImpl(
                store,
                keyCodec,
                valueCodec,
                null
            )
        }
    }
}