package org.tdf.sunflower.state

import org.tdf.common.store.Store
import org.tdf.common.trie.Trie
import org.tdf.common.util.HexBytes
import org.tdf.sunflower.types.Header
import org.tdf.sunflower.vm.Backend

/**
 * state storage
 *
 * @param <ID> identifier of state
 * @param <S>  state
</S></ID> */
interface StateTrie<ID, S> {
    // get an optional state at a root hash
    fun get(rootHash: HexBytes, id: ID): S?

    // init genesis states
    fun init(
        alloc: Map<HexBytes, Account>,
        bios: List<BuiltinContract>,
        builtins: List<BuiltinContract>
    ): HexBytes

    val trie: Trie<ID, S>

    fun getTrie(rootHash: HexBytes): Trie<ID, S>

    val trieStore: Store<ByteArray, ByteArray>

    fun createBackend(
        parent: Header,
        isStatic: Boolean = false,
        root: HexBytes = parent.stateRoot
    ): Backend
}