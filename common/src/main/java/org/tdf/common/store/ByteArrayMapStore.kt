package org.tdf.common.store

import org.tdf.common.util.ByteArrayMap

open class ByteArrayMapStore<V> : MapStore<ByteArray, V> {
    constructor() : super(ByteArrayMap<V>())
    constructor(map: Map<ByteArray, V>) : super(map.toMutableMap())
}