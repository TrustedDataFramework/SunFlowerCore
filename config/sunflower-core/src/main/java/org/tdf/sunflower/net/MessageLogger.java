package org.tdf.sunflower.net;

import lombok.extern.slf4j.Slf4j;

@Slf4j
class MessageLogger implements Plugin {
    @Override
    public void onMessage(ContextImpl context, PeerServerImpl server) {
        log.info("receive {} message from {}:{}",
                context.message.getCode(), context.remote.getHost(), context.remote.getPort()
        );
        log.info(context.message.toString());
    }

    @Override
    public void onStart(PeerServerImpl server) {

    }

    @Override
    public void onNewPeer(PeerImpl peer, PeerServerImpl server) {
        log.info("new peer join {}", peer);
    }

    @Override
    public void onDisconnect(PeerImpl peer, PeerServerImpl server) {
        log.info("peer disconnected {}", peer);
    }
}
