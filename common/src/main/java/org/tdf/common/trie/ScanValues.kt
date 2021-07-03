package org.tdf.common.trie

import org.tdf.common.util.ByteArraySet

internal class ScanValues : ScannerAction {
    val bytes = ByteArraySet()
    override fun scan(path: TrieKey, node: Node): Boolean {
        if (node.type != Node.Type.EXTENSION && node.value != null) {
            bytes.add(node.value)
        }
        return true
    }
}