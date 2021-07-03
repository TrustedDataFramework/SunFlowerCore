package org.tdf.common.store

import org.tdf.common.util.RLPUtil.decode
import org.tdf.common.util.HexBytes
import com.github.salpadding.rlpstream.Rlp
import org.tdf.common.serialize.Codec
import org.tdf.common.util.ByteUtil
import java.util.*

class PrefixStore<K, V>(
    private val contractStorage: Store<HexBytes, HexBytes>,
    private val prefix: HexBytes,
    private val kCodec: Codec<K>,
    private val vCodec: Codec<V>
) : IterableStore<K, V> {
    override fun get(k: K): V? {
        val encoded = kCodec.encoder.apply(k)
        val withPrefix = verifyAndPrefix(encoded)
        val v = contractStorage[withPrefix]
        return if (v == null || v.size() == 0) null else vCodec.decoder.apply(v.bytes)
    }

    private fun verifyAndPrefix(key: ByteArray): HexBytes {
        if (key.size == 0) throw RuntimeException("invalid key, length = 0")
        return HexBytes.fromBytes(ByteUtil.merge(prefix.bytes, key))
    }

    override fun set(k: K, v: V) {
        val encoded = kCodec.encoder.apply(k)
        val withPrefix = verifyAndPrefix(encoded)
        addKey(encoded)
        contractStorage[withPrefix] = HexBytes.fromBytes(vCodec.encoder.apply(v))
    }

    private fun keySet(): TreeSet<HexBytes> {
        val keys = contractStorage[prefix]
        return if (keys == null || keys.size() == 0) TreeSet() else TreeSet(
            Arrays.asList(*decode(keys, Array<HexBytes>::class.java))
        )
    }

    private fun addKey(key: ByteArray) {
        val keySet = keySet()
        keySet.add(HexBytes.fromBytes(key))
        contractStorage[prefix] = HexBytes.fromBytes(Rlp.encode(keySet.toTypedArray()))
    }

    private fun removeKey(key: ByteArray) {
        val keySet = keySet()
        keySet.remove(HexBytes.fromBytes(key))
        contractStorage[prefix] = HexBytes.fromBytes(Rlp.encode(keySet.toTypedArray()))
    }

    override fun remove(k: K) {
        val encoded = kCodec.encoder.apply(k)
        val withPrefix = verifyAndPrefix(encoded)
        removeKey(encoded)
        contractStorage.remove(withPrefix)
    }

    override fun flush() {
        contractStorage.flush()
    }

    override fun iterator(): Iterator<Map.Entry<K, V>> {
        val map: MutableMap<K, V> = HashMap()
        for (key in keySet()) {
            map[kCodec.decoder.apply(key.bytes)] = vCodec.decoder.apply(
                contractStorage[verifyAndPrefix(key.bytes)]!!.bytes
            )
        }
        return map.entries.iterator()
    }
}