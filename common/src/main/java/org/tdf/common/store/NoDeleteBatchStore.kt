package org.tdf.common.store

import java.util.function.Predicate

class NoDeleteBatchStore<K, V>(private val delegate: BatchStore<K, V>, isNull: Predicate<V?>) : NoDeleteStore<K, V>(
    delegate, isNull
), BatchStore<K, V> by delegate{

    override fun putAll(rows: Collection<Map.Entry<K, V>>) {
        delegate.putAll(rows.filter { !isNull.test(it.value) })
    }
}