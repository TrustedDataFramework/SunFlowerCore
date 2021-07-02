package org.tdf.sunflower.net

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import org.slf4j.LoggerFactory
import org.tdf.common.util.FastByteComparisons
import org.tdf.common.util.HashUtil
import org.tdf.common.util.HexBytes
import org.tdf.common.util.LogLock
import org.tdf.sunflower.facade.ConsensusEngine
import org.tdf.sunflower.proto.Code
import org.tdf.sunflower.proto.Message
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import java.util.function.Predicate

/**
 * message filter
 */
class MessageFilter internal constructor(config: PeerServerConfig, consensusEngine: ConsensusEngine) : Plugin {
    private val consensusEngine: ConsensusEngine
    private val cache: Cache<HexBytes, Boolean>
    private val multiPartCache: MutableMap<HexBytes, Messages> = HashMap()
    private val config: PeerServerConfig
    private val multiPartCacheLock: Lock = LogLock(ReentrantLock(), "p2p-mp")
    override fun onMessage(context: ContextImpl, server: PeerServerImpl) {
        // cache multi part message
        if (context.remote.protocol != server.self.protocol) {
            log.error(
                "protocol not match received = {}, while {} expected",
                context.remote.protocol,
                server.self.protocol
            )
            context.block()
            return
        }
        if (context.msg.code == Code.MULTI_PART) {
            multiPartCacheLock.lock()
            val now = System.currentTimeMillis() / 1000
            val key = HexBytes.fromBytes(context.msg.signature.toByteArray())
            try {
                val messages = multiPartCache.getOrDefault(
                    key,
                    Messages(
                        arrayOfNulls(
                            context.msg.ttl.toInt()
                        ),
                        context.msg.ttl.toInt(),
                        now
                    )
                )
                messages.multiParts[context.msg.nonce.toInt()] = context.msg
                multiPartCache[key] = messages
                if (messages.size() == messages.total) {
                    multiPartCache.remove(key)
                    server.onMessage(messages.merge(), context.channel)
                }
            } finally {
                multiPartCacheLock.unlock()
            }
            return
        }

        // filter invalid signatures

        // reject peer in black list and not in whitelist
        if (config.isBlocked(context.remote.id)) {
            log.error("the peer " + context.remote + " has been blocked")
            context.disconnect()
            return
        }

        // reject blocked peer
        if (server.client.peersCache.hasBlocked(context.remote)) {
            log.error("the peer " + context.remote + " has been blocked")
            context.disconnect()
            return
        }
        // filter message from your self
        if (context.remote == server.self) {
            log.error("message received from yourself")
            context.exit()
            return
        }
        // filter message which ttl < 0
        if (context.msg.ttl < 0) {
            log.error("receive message ttl less than 0")
            context.exit()
            return
        }
        val hash = HexBytes.fromBytes(HashUtil.sha3(Util.getRawForSign(context.msg)))

        // filter message had been received
        if (cache.asMap().containsKey(hash)) {
            context.exit()
        }
        log.debug(
            "receive " + context.msg.code
                    + " from " +
                    context.remote.host + ":" + context.remote.port
        )
        cache.put(hash, true)
    }

    override fun onStart(server: PeerServerImpl) {}
    override fun onNewPeer(peer: PeerImpl, server: PeerServerImpl) {}
    override fun onDisconnect(peer: PeerImpl, server: PeerServerImpl) {}
    override fun onStop(server: PeerServerImpl) {}
    private class Messages(val multiParts: Array<Message?>, val total: Int, val writeAt: Long) {
        fun size(): Int {
            return Arrays.stream(multiParts).filter(Predicate { obj: Message? -> Objects.nonNull(obj) }).count()
                .toInt()
        }

        fun merge(): Message {
            val byteArraySize = multiParts.sumOf { it?.body?.size() ?: 0 }
            val total = ByteArray(byteArraySize)
            var current = 0
            for (part in multiParts) {
                val p = part!!.body.toByteArray()
                System.arraycopy(p, 0, total, current, p.size)
                current += p.size
            }
            if (!FastByteComparisons.equal(
                    HashUtil.sha3(total),
                    multiParts[0]!!.signature.toByteArray()
                )
            ) {
                throw RuntimeException("merge failed")
            }
            return Message.parseFrom(total)
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger("net")
    }

    init {
        cache = CacheBuilder.newBuilder()
            .maximumSize((config.maxPeers * 8).toLong()).build()
        this.config = config
        Executors.newSingleThreadScheduledExecutor()
            .scheduleWithFixedDelay({
                multiPartCacheLock.lock()
                val now = System.currentTimeMillis() / 1000
                try {
                    multiPartCache.entries.removeIf { (_, value) -> now - value.writeAt > config.cacheExpiredAfter }
                } finally {
                    multiPartCacheLock.unlock()
                }
            }, config.cacheExpiredAfter.toLong(), config.cacheExpiredAfter.toLong(), TimeUnit.SECONDS)
        this.consensusEngine = consensusEngine
    }
}