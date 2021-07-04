package org.tdf.sunflower.types

import com.github.salpadding.rlpstream.Rlp
import org.tdf.common.store.Store
import org.tdf.common.util.HexBytes
import org.tdf.common.util.RLPUtil
import org.tdf.sunflower.state.hex
import java.util.*

class StorageWrapper(private val prefix: HexBytes, private val store: Store<HexBytes, HexBytes>) {
    constructor(store: Store<HexBytes, HexBytes>) : this(HexBytes.empty(), store)

    private fun verifyAndPrefix(key: HexBytes): HexBytes {
        return prefix.concat(key)
    }

    private fun keySet(): MutableSet<HexBytes> {
        if (prefix.isEmpty) throw UnsupportedOperationException()
        val keys = store[prefix]
        return if (keys == null || keys.isEmpty) TreeSet() else TreeSet(
            RLPUtil.decode(
                keys,
                Array<HexBytes>::class.java
            ).toList()
        )
    }

    private fun addKey(key: HexBytes) {
        val keySet = keySet()
        keySet.add(key)
        store[prefix] = Rlp.encode(keySet).hex()
    }

    fun save(key: HexBytes, o: Any) {
        val prefixed = verifyAndPrefix(key)
        // if this is prefix store, add key
        if (!prefix.isEmpty) {
            addKey(key)
        }
        store[prefixed] = RLPUtil.encode(o)
    }

    private fun removeKey(key: HexBytes) {
        val keySet = keySet()
        keySet.remove(key)
        store[prefix] = HexBytes.fromBytes(Rlp.encode(keySet))
    }

    fun remove(key: HexBytes) {
        val prefixed = verifyAndPrefix(key)
        // if this is prefix store, add key
        if (!prefix.isEmpty) {
            removeKey(key)
        }
        store.remove(prefixed)
    }

    operator fun <T> get(key: HexBytes, clazz: Class<T>, defaultValue: T): T {
        val prefixed = verifyAndPrefix(key)
        val h = store[prefixed] ?: return defaultValue
        return RLPUtil.decode(h, clazz)
    }

    fun <T> getList(key: HexBytes, clazz: Class<T>, defaultValue: MutableList<T>?): MutableList<T>? {
        val prefixed = verifyAndPrefix(key)
        val h = store[prefixed]
        if (h == null || h.isEmpty) return defaultValue
        val ret: MutableList<T> = ArrayList()
        val li = Rlp.decodeList(h.bytes)
        for (i in 0 until li.size()) {
            ret.add(
                Rlp.decode(li.rawAt(i), clazz)
            )
        }
        return ret
    }

    fun <T : Comparable<T>> getSet(key: HexBytes, clazz: Class<T>, defaultValue: MutableSet<T>?): MutableSet<T>? {
        val prefixed = verifyAndPrefix(key)
        val h = store[prefixed]
        if (h == null || h.isEmpty) return defaultValue
        val ret = TreeSet<T>()
        val li = Rlp.decodeList(h.bytes)
        for (i in 0 until li.size()) {
            ret.add(
                Rlp.decode(li.rawAt(i), clazz)
            )
        }
        return ret
    }
}