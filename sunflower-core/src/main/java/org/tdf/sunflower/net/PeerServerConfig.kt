package org.tdf.sunflower.net

import org.tdf.common.crypto.ECKey
import org.tdf.common.util.HexBytes
import org.tdf.sunflower.types.PropertyReader
import java.net.URI

class PeerServerConfig(private val reader: PropertyReader) {
    companion object {
        const val PREFIX = ""
    }

    val name: String
        get() {
            val raw = reader.getAsLowerCased("name")
            return raw.takeIf { it == "grpc" || it == "websocket" }
                ?: throw RuntimeException("invalid p2p name $raw expecting grpc or websocket")
        }

    private fun PropertyReader.getAsPositive(key: String): Int {
        val raw = reader.getAsInt(key)
        return raw.takeIf { it > 0 } ?: throw RuntimeException("invalid $key = $raw, should be positive")
    }

    val maxPeers: Int by lazy { reader.getAsPositive("max-peers") }

    val maxTTL: Int by lazy { reader.getAsPositive("max-ttl") }

    val discovery: Boolean by lazy { reader.getAsBool("enable-discovery") }

    val address: URI get() = URI(reader.getAsNonNull("address"))

    val bootstraps: List<URI> by lazy { reader.getAsList("bootstraps").map { URI(it) } }
    val trusted: List<URI> by lazy { reader.getAsList("trusted").map { URI(it) } }

    val persist: Boolean get() = reader.getAsBool("persist")
    val discoverRate: Int get() = reader.getAsPositive("discover-rate")
    val maxPacketSize: Int by lazy { reader.getAsPositive("max-packet-size") }
    val cacheExpiredAfter: Int by lazy { reader.getAsPositive("cache-expired-after") }
    val privateKey: HexBytes by lazy { reader.getAsPrivate("private-key") ?: HexBytes.fromBytes(ECKey().privKeyBytes) }
}