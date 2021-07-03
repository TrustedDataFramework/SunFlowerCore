package org.tdf.common.store

import java.util.function.Predicate

class NoDoubleDeleteStore<K, V>(private val delegate: Store<K, V>, private val isNull: Predicate<V?>) : Store<K, V> by delegate {
    override fun remove(k: K) {
        val v = get(k)
        if (isNull.test(v)) throw RuntimeException("trying to delete a non-exists key")
        delegate.remove(k)
    }
}