package org.tdf.sunflower.p2pv2.server

import io.netty.channel.ChannelInitializer
import io.netty.channel.socket.nio.NioSocketChannel

abstract class PeerChannelInitializer : ChannelInitializer<NioSocketChannel>() {
    var peerDiscoveryMode = false

    // set remoteId when create outbound channel
    var remoteId: String = ""
}