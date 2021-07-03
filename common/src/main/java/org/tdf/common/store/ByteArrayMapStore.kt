package org.tdf.common.store

import org.tdf.common.util.ByteArrayMap

open class ByteArrayMapStore<V> : MapStore<ByteArray, V> {
    constructor() : super(ByteArrayMap<V>())
    constructor(map: MutableMap<ByteArray, V>) : super(map)
}