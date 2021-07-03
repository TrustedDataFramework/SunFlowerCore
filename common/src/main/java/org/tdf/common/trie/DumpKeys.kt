package org.tdf.common.trie

import org.tdf.common.util.HexBytes
import java.util.HashSet

internal class DumpKeys : ScannerAction {
    internal val keys: MutableSet<HexBytes> = HashSet()

    override fun scan(path: TrieKey, node: Node): Boolean {
        if (node.hash != null) {
            keys.add(HexBytes.fromBytes(node.hash))
        }
        return true
    }
}