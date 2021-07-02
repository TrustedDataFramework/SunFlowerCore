package org.tdf.sunflower.net

import org.slf4j.LoggerFactory
import org.tdf.sunflower.proto.Code
import org.tdf.sunflower.proto.Message
import java.net.URI
import java.util.function.Consumer

class Client(
    self: PeerImpl,
    config: PeerServerConfig,
    messageBuilder: MessageBuilder,
    netLayer: NetLayer
) : ChannelListener {
    private val config: PeerServerConfig
    private val netLayer: NetLayer
    private val self: PeerImpl
    private val builder: MessageBuilder
    var messageBuilder: MessageBuilder
    var peersCache: PeersCache

    // listener for channel event
    private var listener = ChannelListener.NONE
    fun withListener(listener: ChannelListener): Client {
        this.listener = listener
        return this
    }

    fun broadcast(message: Message) {
        peersCache.channels.forEach { it.write(message) }
    }

    fun dial(peer: Peer, message: Message) {
        getChannel(peer)?.write(message)
    }

    fun dial(host: String, port: Int, message: Message) {
        getChannel(host, port, this, listener)?.write(message)
    }

    fun bootstrap(uris: Collection<URI>) {
        for (uri in uris) {
            connect(uri.host, uri.port) { peersCache.bootstraps[it] = true }
        }
    }

    fun trust(trusted: Collection<URI>) {
        for (uri in trusted) {
            connect(uri.host, uri.port) { peersCache.trusted[it] = true }
        }
    }

    // functional interface for connect to bootstrap and trusted peer
    // consumer may be called more than once
    // usually called when server starts
    private fun connect(host: String, port: Int, connectionConsumer: Consumer<PeerImpl>) {
        val remote = getChannel(host, port, listener, object : ChannelListener {
            override fun onConnect(remote: PeerImpl, channel: Channel) {
                connectionConsumer.accept(remote)
                addPeer(remote, channel)
            }

            override fun onMessage(message: Message, channel: Channel) {}
            override fun onError(throwable: Throwable, channel: Channel) {}
            override fun onClose(channel: Channel) {}
        })?.remote // if the connection had already created, onConnect will not triggered
        // but the peer will be handled here
        remote?.let { connectionConsumer.accept(it) }
    }

    // try to get channel from cache, if channel not exists in cache,
    // create from net layer
    private fun getChannel(peer: Peer): Channel? {
        // cannot create channel connect to your self
        if (peer == self) return null
        val ch = peersCache
            .getChannel(peer.id)
        if (ch != null)
            return ch
        return netLayer
            .createChannel(peer.host, peer.port, this, listener)
            ?.takeIf { it.isAlive }
    }

    // try to get channel from cache, if channel not exists in cache,
    // create from net layer
    private fun getChannel(host: String, port: Int, vararg listeners: ChannelListener): Channel? {
        var ch = peersCache.channels
            .firstOrNull {
                it.remote?.host == host && it.remote?.port == port
            }

        if (ch != null) return ch
        ch = netLayer
            .createChannel(host, port, *listeners)
            ?.takeIf { it.isAlive }
        ch?.write(messageBuilder.buildPing())
        return ch
    }

    override fun onConnect(remote: PeerImpl, channel: Channel) {
        if (!config.isEnableDiscovery &&
            !peersCache.bootstraps.containsKey(remote) &&
            !peersCache.trusted.containsKey(remote)
        ) {
            channel.close("discovery is not enabled accept bootstraps and trusted only")
            return
        }
        addPeer(remote, channel)
    }

    private fun addPeer(remote: PeerImpl, channel: Channel) {
        if (remote == self) {
            channel.close("close channel connect to self")
            return
        }
        val o = peersCache.getChannel(remote)
        if (o != null && o.isAlive) {
            log.error("the channel to $remote had been created")
            return
        }
        peersCache.keep(remote, channel)
    }

    override fun onMessage(message: Message, channel: Channel) {}
    override fun onError(throwable: Throwable, channel: Channel) {
        throwable.printStackTrace()
        channel.remote
            ?.let {
                peersCache.half(it)
                log.error("error found decrease the score of peer " + it + " " + throwable.message)
            }
    }

    override fun onClose(channel: Channel) {
        channel.remote?.let {
            peersCache.remove(it.id, " channel closed")
        }
    }

    fun relay(message: Message, receivedFrom: PeerImpl?) {
        peersCache.channels
            .filter { it.remote != null && it.remote != receivedFrom }
            .forEach {
                if (message.code == Code.ANOTHER) {
                    val msg = message.body.toByteArray()
                    it.write(builder.buildMessage(Code.ANOTHER, message.ttl - 1, msg))
                    return@forEach
                }
                it.write(messageBuilder.buildRelay(message))
            }
    }

    init {
        peersCache = PeersCache(self, config)
        this.config = config
        this.messageBuilder = messageBuilder
        this.netLayer = netLayer
        this.self = self
        builder = MessageBuilder(self, config)
    }

    companion object {
        val log = LoggerFactory.getLogger("net")
    }
}