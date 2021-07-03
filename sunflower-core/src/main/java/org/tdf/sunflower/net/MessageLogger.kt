package org.tdf.sunflower.net

import org.slf4j.LoggerFactory

internal class MessageLogger : Plugin {
    override fun onMessage(context: ContextImpl, server: PeerServerImpl) {
        log.debug(
            "receive {} message from {}:{}",
            context.msg.code, context.remote.host, context.remote.port
        )
        log.debug(context.message.toString())
    }

    override fun onStart(server: PeerServerImpl) {}
    override fun onNewPeer(peer: PeerImpl, server: PeerServerImpl) {
        log.info("new peer join {}", peer)
    }

    override fun onDisconnect(peer: PeerImpl, server: PeerServerImpl) {
        log.info("peer disconnected {}", peer)
    }

    override fun onStop(server: PeerServerImpl) {}

    companion object {
        private val log = LoggerFactory.getLogger("net")
    }
}