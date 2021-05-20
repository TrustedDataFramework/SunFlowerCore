package org.tdf.sunflower.p2pv2.client

import io.netty.channel.ChannelFuture
import java.io.Closeable

interface PeerClient: Closeable{
    fun connect(host: String, port: Int, remoteId: String, discoveryMode: Boolean = false)
    fun connectAsync(host: String, port: Int, remoteId: String, discoveryMode: Boolean = false): ChannelFuture
    override fun close()
}