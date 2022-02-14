package org.tdf.sunflower.net

import com.github.salpadding.rlpstream.Rlp
import com.github.salpadding.rlpstream.annotation.RlpCreator
import com.github.salpadding.rlpstream.annotation.RlpProps
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.tdf.sunflower.proto.Code
import org.tdf.sunflower.proto.Message
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.URI
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Consumer

@RlpProps("code", "n", "uri")
data class PingPong @RlpCreator constructor(val code: Int, val n: Int, val uri: String)

class Client(
    val self: PeerImpl,
    val config: PeerServerConfig,
    val builder: MessageBuilder,
    private val netLayer: NetLayer,
) : ChannelListener, AutoCloseable {
    private val buf: ByteArray = ByteArray(256)
    private val tailBuf: ByteArray = ByteArray(512)
    val serverSocket = DatagramSocket(self.port)

    val ex = Executors.newSingleThreadExecutor()
    val n = AtomicInteger()

    @Volatile
    private var udpListening: Boolean = true

    val peersCache: PeersCache = PeersCache(self, config)

    // listener for channel event
    var listener = ChannelListener.NONE

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
            log.info("bootstrap peer {}", uri.toString())
            pingUdp(uri.host, uri.port) { peersCache.bootstraps[it] = true }
        }
    }

    fun trust(trusted: Collection<URI>) {
        for (uri in trusted) {
            log.info("bootstrap trusted peer {}", uri.toString())
            pingUdp(uri.host, uri.port) { peersCache.trusted[it] = true }
        }
    }

    // functional interface for connect to bootstrap and trusted peer
    // consumer may be called more than once
    // usually called when server starts
    private fun connect(host: String, port: Int, cb: Consumer<PeerImpl>) {
        val remote = getChannel(host, port, listener, object : ChannelListener {
            override fun onConnect(remote: PeerImpl, channel: Channel) {
                cb.accept(remote)
                addPeer(remote, channel)
            }

            override fun onMessage(message: Message, channel: Channel) {}
            override fun onError(throwable: Throwable, channel: Channel) {}
            override fun onClose(channel: Channel) {}
        })?.remote
        // if the connection had already created, onConnect will not triggered
        // but the peer will be handled here
        remote?.let { cb.accept(it) }
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

        ch?.write(builder.buildPing())
        return ch?.takeIf { it.isAlive }
    }

    override fun onConnect(remote: PeerImpl, channel: Channel) {
        if (!config.discovery &&
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

    fun relay(message: Message, receivedFrom: PeerImpl) {
        peersCache.channels
            .filter { it.remote != null && it.remote != receivedFrom }
            .forEach {
                if (message.code == Code.ANOTHER) {
                    val msg = message.body.toByteArray()
                    it.write(builder.buildMessage(Code.ANOTHER, message.ttl - 1, msg))
                    return@forEach
                }
                it.write(builder.buildRelay(message))
            }
    }

    // val a = InetAddress.getByName(host)
// val packet = DatagramPacket(ping, ping.size, a, port)
    companion object {
        val log: Logger = LoggerFactory.getLogger("net")
    }

    private val maxHandlers = 1024
    val handlers: Array<Pair<Int, Consumer<PeerImpl>>?> = arrayOfNulls(maxHandlers)

    // ping and get channel
    private fun pingUdp(host: String, port: Int, handle: Consumer<PeerImpl>) {
        val a = InetAddress.getByName(host)
        val nonce = n.incrementAndGet()
        handlers[nonce % maxHandlers] = Pair(nonce, handle)
        val b = PingPong(0, nonce, self.encodeURI())
        val buf = Rlp.encode(b)
        val p = DatagramPacket(buf, buf.size, a, port)
        log.info("send packet {} to {} {}", b, p.address, p.port)
        serverSocket.send(p)
    }

    private fun handlePingPong() {
        while (udpListening) {
            try {
                val req = DatagramPacket(buf, buf.size)
                serverSocket.receive(req)

                val address: InetAddress = req.address
                val port: Int = req.port

                System.arraycopy(buf, 0, tailBuf, tailBuf.size - req.length, req.length)
                val p = Rlp.decode(tailBuf, tailBuf.size - req.length, PingPong::class.java)
                log.info("receive {} from {} {}", p, req.address, req.port)

                if (p.code == 0) {
                    val pong = PingPong(1, p.n, self.encodeURI())
                    val e = Rlp.encode(pong)
                    val resp = DatagramPacket(e, e.size, address, port)
                    log.info("send {} to {} {}", pong, address, port)
                    serverSocket.send(resp)
                    continue
                }

                val peer = PeerImpl.parse(p.uri) ?: continue
                val h = handlers[p.n % maxHandlers] ?: continue
                if (h.first != p.n) continue

                handlers[p.n % maxHandlers] = null
                val ch = peersCache
                    .getChannel(peer.id)

                if (ch != null) {
                    h.second.accept(peer)
                    continue
                }

                val ft = CompletableFuture.supplyAsync {
                    netLayer.createChannel(peer.host, peer.port, this, listener)
                }

                ft.thenAccept {
                    if (it != null) h.second.accept(peer)
                }
                continue
            } catch (e: Exception) {
                e.printStackTrace()
                Thread.sleep(100)
            }
        }
    }

    init {
        ex.submit {
            this.handlePingPong()
        }
    }

    override fun close() {
        udpListening = false
        serverSocket.close()
        ex.shutdown()
    }
}