package org.tdf.sunflower.net

import org.tdf.sunflower.proto.Code
import org.tdf.sunflower.proto.Message

class ContextImpl internal constructor(
    override val remote: PeerImpl,
    val msg: Message,
    val channel: Channel,
    val builder: MessageBuilder,
    val client: Client,
) : Context {
    private var relayed: Boolean = false
    var exited: Boolean = false
        private set
    private var disconnected: Boolean = false
    private var blocked: Boolean = false
    private var decrypted: ByteArray? = null

    override fun exit() {
        exited = true
    }

    override fun disconnect() {
        if (exited || blocked || disconnected) return
        disconnected = true
        client.peersCache.remove(remote.id, " disconnect to $remote by listener")
    }

    override fun block() {
        if (exited || blocked) return
        blocked = true
        disconnected = true
        client.peersCache.block(remote)
    }

    override fun keep() {
        if (exited || blocked || disconnected) return
        client.peersCache.keep(remote, channel)
    }

    override fun response(message: ByteArray) {
        response(setOf(message))
    }

    override fun response(messages: Collection<ByteArray>) {
        if (exited || blocked || disconnected) return
        for (msg in messages) {
            builder.buildAnother(msg, 1, remote)
                .forEach { channel.write(it) }
        }
    }

    override fun relay() {
        if (exited || blocked || disconnected || relayed || msg.ttl == 0L) return
        relayed = true
        client.relay(msg, remote)
    }

    override val message: ByteArray
        get() {
            if (msg.code == Code.ANOTHER) {
                if (decrypted != null) return decrypted!!
                decrypted = msg.body.toByteArray()
                return decrypted!!
            }
            return msg.body.toByteArray()
        }
}