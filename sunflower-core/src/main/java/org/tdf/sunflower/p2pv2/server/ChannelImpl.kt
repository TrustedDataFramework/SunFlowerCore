package org.tdf.sunflower.p2pv2.server

import io.netty.channel.ChannelPipeline
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import java.net.InetSocketAddress

@Component
@Scope("prototype")
class ChannelImpl @Autowired constructor(
) : Channel {
    override fun init(
        pipeline: ChannelPipeline,
        remoteId: String,
        discoveryMode: Boolean,
        channelManager: ChannelManager
    ) {
        TODO("Not yet implemented")
    }

    override var inetSocketAddress: InetSocketAddress? = null
    override fun initWithNode(nodeId: ByteArray?, remotePort: Int) {
        TODO("Not yet implemented")
    }

    override fun initWithNode(nodeId: ByteArray?) {
        TODO("Not yet implemented")
    }
}