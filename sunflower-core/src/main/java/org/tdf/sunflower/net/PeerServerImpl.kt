package org.tdf.sunflower.net

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsMapper
import lombok.extern.slf4j.Slf4j
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.tdf.common.crypto.ECKey
import org.tdf.common.store.JsonStore
import org.tdf.sunflower.facade.ConsensusEngine
import org.tdf.sunflower.facade.PeerServerListener
import org.tdf.sunflower.proto.Message
import org.tdf.sunflower.util.MapperUtil
import java.io.IOException
import java.net.URI
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import java.util.function.Consumer
import java.util.stream.Collectors
import java.util.stream.Stream

@Slf4j(topic = "net")
class PeerServerImpl(// if non-database provided, use memory database
    val peerStore: JsonStore,
    val consensusEngine: ConsensusEngine,
    val properties: Properties
) : ChannelListener, PeerServer {
    private val plugins: MutableList<Plugin> = CopyOnWriteArrayList()
    private var config: PeerServerConfig

    val client: Client
    private val self: PeerImpl
    private val builder: MessageBuilder
    private val netLayer: NetLayer

    override fun isFull(): Boolean {
        return client.peersCache.isFull
    }

    override fun getSelf(): Peer {
        return self
    }

    override fun dial(peer: Peer, message: ByteArray) {
        builder.buildAnother(message, 1, peer)
            .forEach(Consumer { m: Message? -> client.dial(peer, m) })
    }

    override fun broadcast(message: ByteArray) {
        client.peersCache.channels
            .filter { ch: Channel -> ch.remote.isPresent }
            .forEach { ch: Channel ->
                builder.buildAnother(message, config.maxTTL, ch.remote.get())
                    .forEach(Consumer { message: Message? -> ch.write(message) })
            }
    }

    override fun getBootStraps(): List<Peer> {
        return ArrayList<Peer>(client.peersCache.bootstraps.keys)
    }

    override fun getPeers(): List<Peer> {
        return client.peersCache.peers.collect(Collectors.toList())
    }

    override fun addListeners(vararg peerServerListeners: PeerServerListener) {
        for (listener in peerServerListeners) {
            plugins.add(PluginWrapper(listener))
        }
    }

    override fun start() {
        plugins.forEach { it.onStart(this) }
        netLayer.start()
        resolveHost()
        log.info(
            "peer server is listening on " +
                    self.encodeURI()
        )
        if (config.bootstraps != null) {
            client.bootstrap(config.bootstraps)
        }
        if (config.trusted != null) {
            client.trust(config.trusted)
        }
        // connect to stored peers when server restarts
        peerStore.forEach{ k: Map.Entry<String, JsonNode> ->
            if ("self" == k.key)
                return
            val peer = PeerImpl.parse(k.value.asText()).get()
            client.dial(peer.host, peer.port, builder.buildPing())
        }
    }

    init {
        val mapper = MapperUtil.PROPS_MAPPER;
        try {
            config = mapper.readPropertiesAs(properties, PeerServerConfig::class.java)
            if (config.maxTTL <= 0)
                config.maxTTL = PeerServerConfig.DEFAULT_MAX_TTL
            if (config.maxPeers <= 0)
                config.maxPeers = PeerServerConfig.DEFAULT_MAX_PEERS
        } catch (e: Exception) {
            var schema = ""
            try {
                // create a example properties for error log
                schema = mapper.writeValueAsProperties(
                    PeerServerConfig.builder()
                        .bootstraps(listOf(URI("node://localhost:9955")))
                        .build()
                ).toString()
            } catch (ignored: Exception) {
            }
            throw RuntimeException(
                "load properties failed :$properties expecting $schema"
            )
        }
        if (!config.isEnableDiscovery &&
            Stream.of(config.bootstraps, config.trusted)
                .filter { obj: List<URI?>? -> Objects.nonNull(obj) }
                .map { obj: List<URI?> -> obj.size }
                .reduce(0) { a: Int, b: Int -> Integer.sum(a, b) } == 0
        ) {
            log.warn(
                "cannot connect to any peer for the discovery " +
                        "is disabled and none bootstraps and trusted provided"
            )
        }
        try {
            // find valid private key from 1.properties 2.persist 3. generate
            var sk = if (config.privateKey == null) null else config.privateKey.bytes
            if (sk == null || sk.isEmpty()) {
                sk = ECKey().privKeyBytes
            }
            self = PeerImpl.createSelf(config.address, sk)
        } catch (e: Exception) {
            e.printStackTrace()
            throw RuntimeException("failed to load peer server invalid address " + config.getAddress())
        }
        builder = MessageBuilder(self, config)
        netLayer = if ("grpc" == config.name.trim { it <= ' ' }.lowercase()) {
            GRpcNetLayer(self.port, builder)
        } else {
            WebSocketNetLayer(self.port, builder)
        }
        client = Client(self, config, builder, netLayer).withListener(this)
        netLayer.setHandler { c: Channel -> c.addListeners(client, this) }

        // loading plugins
        plugins.add(MessageFilter(config, consensusEngine))
        plugins.add(MessageLogger())
        plugins.add(PeersManager(config))
    }

    override fun onConnect(remote: PeerImpl, channel: Channel) {
        for (plugin in plugins) {
            plugin.onNewPeer(remote, this)
        }
    }

    override fun onMessage(message: Message, channel: Channel) {
        val peer = channel.remote
        if (!peer.isPresent) {
            channel.close("failed to parse peer " + message.remotePeer)
            throw RuntimeException("failed to parse peer")
        }
        val context = ContextImpl.builder()
            .channel(channel)
            .client(client)
            .message(message)
            .builder(builder)
            .remote(peer.get()).build()
        for (plugin in plugins) {
            if (context.exited) break
            try {
                plugin.onMessage(context, this)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onError(throwable: Throwable, channel: Channel) {}
    override fun onClose(channel: Channel) {
        if (channel.remote.isPresent) {
            for (plugin in plugins) {
                plugin.onDisconnect(channel.remote.get(), this)
            }
        }
    }

    private fun resolveHost() {
        if (self.host != "localhost" && self.host != "127.0.0.1") {
            return
        }
        var externalIP: String? = null
        try {
            externalIP = Util.externalIp()
        } catch (ignored: Exception) {
            log.error("cannot get external ip, fall back to bind ip")
        }
        if (externalIP != null && Util.ping(externalIP, self.port)) {
            log.info("ping $externalIP success, set as your host")
            self.host = externalIP
            return
        }
        var bindIP: String? = null
        try {
            bindIP = Util.bindIp()
        } catch (e: Exception) {
            log.error("get bind ip failed")
        }
        if (bindIP != null) {
            self.host = bindIP
        }
    }

    override fun stop() {
        plugins.forEach { x: Plugin -> x.onStop(this) }
        client.peersCache
            .channels
            .forEach { x: Channel -> x.close("application will shutdown") }
        try {
            netLayer.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        log.info("peer server closed")
    }

    companion object {
        val log: Logger = LoggerFactory.getLogger("p2p")
    }
}