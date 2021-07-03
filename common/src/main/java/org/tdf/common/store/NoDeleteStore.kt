package org.tdf.common.store

import java.util.function.Predicate

/**
 * no delete store will store deleted key-value pair to @see deleted
 * when compact method called, clean the key-pari in @see deleted
 */
open class NoDeleteStore<K, V>(
    private val delegate: Store<K, V>,
    val isNull: Predicate<V?>
) : Store<K, V> by delegate {

    override fun set(k: K, v: V) {
        if (isNull.test(v)) return
        delegate[k] = v
    }

    override fun remove(k: K) {}
}