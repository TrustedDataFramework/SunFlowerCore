package org.tdf.sunflower.net;

// Plugin for message handling
interface Plugin {
    void onMessage(ContextImpl context, PeerServerImpl server);

    void onStart(PeerServerImpl server);

    void onNewPeer(PeerImpl peer, PeerServerImpl server);

    void onDisconnect(PeerImpl peer, PeerServerImpl server);
}
