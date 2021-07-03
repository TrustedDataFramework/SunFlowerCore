package org.tdf.common.trie


import org.tdf.common.store.Store
import org.tdf.common.util.HexBytes
import java.util.function.BiFunction
import java.util.function.Function

/**
 * https://medium.com/codechain/secure-tree-why-state-tries-key-is-256-bits-1276beb68485
 *
 *
 * That is, if there are new nodes added into, a modification of, or an attempt to read these two tries,
 * there must be a disk IO, and if possible, must pass by the least amount of nodes possible until reaching the leaf node.
 * For this reason, MPT allowed for the compressing of the 1-child branch node with the extension node.
 * However, only branch nodes with a single child can be compressed into an extension node.
 * Thus, if an attacker can maliciously create a branch node with two children, he/she can attack at a low cost.
 * Thus, the secure tree uses a keccak-256 hash value as its key, and prevents an attacker from creating a node at a location that he/she desires.
 *
 * @param <V> value type
</V> */
class SecureTrie<K, V>(delegate: Trie<K, V>) : Trie<K, V> by delegate {
    private val delegate: AbstractTrie<K, V>

    override fun revert(rootHash: HexBytes, store: Store<ByteArray, ByteArray>): Trie<K, V> {
        return SecureTrie(delegate.revert(rootHash, store))
    }

    override fun revert(rootHash: HexBytes): Trie<K, V> {
        return SecureTrie(delegate.revert(rootHash))
    }

    override fun revert(): Trie<K, V> {
        return SecureTrie(delegate.revert())
    }


    override fun traverse(traverser: BiFunction<in K, in V, Boolean>) {
        throw UnsupportedOperationException("not supported in secure trie")
    }


    override fun traverseValue(traverser: Function<in V, Boolean>) {
        delegate.traverseInternal { k: ByteArray?, v: ByteArray? ->
            traverser.apply(
                delegate.vCodec.decoder.apply(v!!)
            )
        }
    }

    inner class ValueOnlyEntry(override val value: V) : MutableMap.MutableEntry<K, V> {
        override val key: K
            get() {
                throw UnsupportedOperationException()
            }

        override fun setValue(newValue: V): V {
            throw UnsupportedOperationException()
        }
    }

    init {
        this.delegate = delegate as AbstractTrie<K, V>
    }
}