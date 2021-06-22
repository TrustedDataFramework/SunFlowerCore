package org.tdf.sunflower.net

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.TextNode
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.tdf.common.store.BatchStore
import org.tdf.common.util.FixedDelayScheduler
import org.tdf.sunflower.proto.Code
import org.tdf.sunflower.proto.Disconnect
import org.tdf.sunflower.proto.Peers
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.stream.Stream
import kotlin.streams.toList

class PeersManager internal constructor(private val config: PeerServerConfig) : Plugin {
    private val pending: MutableMap<PeerImpl, Boolean> = ConcurrentHashMap()
    private var server: PeerServerImpl? = null


    override fun onMessage(context: ContextImpl, server: PeerServerImpl) {
        val client = server.client
        val cache = client.peersCache
        val builder = client.messageBuilder
        context.keep()
        when (context.message.code) {
            Code.PING -> {
                context.channel.write(builder.buildPong())
                return
            }
            Code.LOOK_UP -> {
                context.channel.write(
                    builder.buildPeers(server.peers)
                )
                return
            }
            Code.PEERS -> {
                if (!config.isEnableDiscovery) return

                Peers.parseFrom(context.message.body).peersList.stream()
                    .map { url: String? -> PeerImpl.parse(url) }
                    .filter { obj: Optional<PeerImpl?> -> obj.isPresent }
                    .map { obj: Optional<PeerImpl?> -> obj.get() }
                    .filter { x: PeerImpl -> !cache.contains(x) && x != server.self && x.protocol == server.self.protocol }
                    .forEach { x: PeerImpl -> pending[x] = true }
                return
            }
            Code.DISCONNECT -> {
                val reason = Disconnect.parseFrom(context.message.body).reason
                if (reason != null && !reason.isEmpty()) PeersManager.log.error("disconnect from peer " + context.getRemote() + " reason is " + reason)
                context.channel.close()
                return
            }
            else -> {

            }
        }
    }

    override fun onStart(server: PeerServerImpl) {
        this.server = server
        val client = server.client
        val cache = client.peersCache
        val builder = client.messageBuilder

        val pingTicker = FixedDelayScheduler("PeersManager-Ping", config.discoverRate.toLong())
        // keep self alive
        pingTicker.delay {
            try {
                client.broadcast(builder.buildPing())
            }catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val loopTicker = FixedDelayScheduler("PeersManager-Discover", config.discoverRate.toLong())
        loopTicker.delay {
            try {
                loopPeers(server, client, cache, builder)
            }catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun loopPeers(server: PeerServerImpl, client: Client, cache: PeersCache, builder: MessageBuilder) {
        // persist peers
        (server.peerStore as BatchStore<String?, JsonNode?>)
            .putAll(
                client.peersCache.peers
                    .map { AbstractMap.SimpleEntry(it.id.toHex(), TextNode(it.encodeURI())) }
                    .toList()
            )
        lookup()
        cache.half()
        if (!config.isEnableDiscovery)
            return
        pending.keys
            .stream()
            .filter { x: PeerImpl? -> !cache.contains(x) }
            .limit(config.maxPeers.toLong())
            .forEach { p: PeerImpl ->
                log.info("try to connect to peer $p")
                client.dial(p, builder.buildPing())
            }
        pending.clear()
    }

    private fun lookup() {
        val client = server!!.client
        val cache = client.peersCache
        val builder = client.messageBuilder
        if (!config.isEnableDiscovery) {
            // keep channel to bootstraps and trusted alive
            val keys = cache.bootstraps.keys + cache.trusted.keys
            for (key in keys) {
                if (cache.contains(key))
                    continue
                server!!.client.dial(key, builder.buildPing())
            }
            return
        }
        // query for neighbours when neighbours is not empty
        if (cache.size() > 0) {
            client.broadcast(builder.buildLookup())
            cache.trusted.keys.forEach { client.dial(it, builder.buildPing()) }
            return
        }
        // query for peers from bootstraps and trusted when neighbours is empty
        Stream.of(cache.bootstraps, cache.trusted)
            .flatMap { x: Map<PeerImpl?, Boolean?> -> x.keys.stream() }
            .forEach { p: PeerImpl? -> client.dial(p, builder.buildLookup()) }
    }

    override fun onNewPeer(peer: PeerImpl, server: PeerServerImpl) {}
    override fun onDisconnect(peer: PeerImpl, server: PeerServerImpl) {}

    override fun onStop(server: PeerServerImpl) {

    }

    companion object {
        val log: Logger = LoggerFactory.getLogger("net")
    }
}