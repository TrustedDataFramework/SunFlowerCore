package org.tdf.sunflower.net

import org.tdf.sunflower.proto.Message

/**
 * channel for message transports
 */
interface Channel {
    /**
     * write message to channel
     */
    fun write(message: Message)

    /**
     * close the channel
     */
    fun close() {
        close("")
    }

    /**
     * close the channel
     */
    fun close(reason: String)
    val isAlive: Boolean
        get() = !isClosed


    /**
     * whether the channel is closed
     */
    val isClosed: Boolean

    /**
     * notify listeners new message received
     */
    fun message(message: Message)

    /**
     * notify listeners error
     */
    fun error(throwable: Throwable)

    val remote: PeerImpl?

    // bind listener to the channel
    fun addListeners(vararg listeners: ChannelListener)
}