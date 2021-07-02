package org.tdf.sunflower.net

import org.slf4j.LoggerFactory
import org.tdf.sunflower.net.PeerImpl.Companion.parse
import org.tdf.sunflower.proto.Message
import java.util.concurrent.CopyOnWriteArrayList

// communicating channel with peer
class ProtoChannel(private val messageBuilder: MessageBuilder) : Channel {
    @Volatile
    override var isClosed = false
        private set

    override var remote: PeerImpl? = null
    private var out: ChannelOut? = null

    @Volatile
    private var pinged = false
    private var listeners: MutableList<ChannelListener> = CopyOnWriteArrayList()

    fun setOut(out: ChannelOut) {
        this.out = out
    }

    override fun message(message: Message) {
        if (isClosed) return
        handlePing(message)

        listeners.forEach {
            if (isClosed) return
            it.onMessage(message, this)
        }
    }

    private fun handlePing(message: Message) {
        if (pinged) return
        remote = parse(message.remotePeer)
        if (remote == null) {
            close("invalid peer " + message.remotePeer)
            return
        }
        pinged = true

        listeners.forEach {
            if (isClosed) return
            it.onConnect(remote, this)
        }
    }

    override fun error(throwable: Throwable) {
        if (isClosed) return
        for (listener in listeners) {
            if (isClosed) return
            listener.onError(throwable, this)
        }
    }

    override fun close(reason: String) {
        if (isClosed) return
        isClosed = true
        if (reason.isNotEmpty()) {
            out!!.write(messageBuilder.buildDisconnect(reason))
            log.error("close channel to $remote reason is $reason")
        }
        listeners.forEach { it.onClose(this) }
        listeners.clear()

        try {
            out!!.close()
        } catch (ignore: Exception) {
        }
    }

    override fun write(message: Message) {
        if (isClosed) {
            log.error("the channel is closed")
            return
        }
        try {
            out!!.write(message)
        } catch (e: Throwable) {
            e.printStackTrace()
            log.error(e.message)
            error(e)
        }
    }


    override fun addListeners(vararg listeners: ChannelListener) {
        this.listeners.addAll(listeners)
    }

    interface ChannelOut {
        fun write(message: Message)
        fun close()
    }

    companion object {
        private val log = LoggerFactory.getLogger("net")
    }
}