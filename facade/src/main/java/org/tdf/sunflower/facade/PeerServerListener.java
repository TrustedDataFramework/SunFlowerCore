package org.tdf.sunflower.facade;

import org.tdf.sunflower.net.Context;
import org.tdf.sunflower.net.Peer;
import org.tdf.sunflower.net.PeerServer;

public interface PeerServerListener {
    // triggered when new message received
    void onMessage(Context context, PeerServer server);

    // triggered when server starts, you could run scheduled task here
    // you could set the PeerServer as your member here
    void onStart(PeerServer server);

    // triggered when a new peer connected
    void onNewPeer(Peer peer, PeerServer server);

    // triggered when a peer disconnected
    void onDisconnect(Peer peer, PeerServer server);

    PeerServerListener NONE = new PeerServerListener() {
        @Override
        public void onMessage(Context context, PeerServer server) {

        }

        @Override
        public void onStart(PeerServer server) {

        }

        @Override
        public void onNewPeer(Peer peer, PeerServer server) {

        }

        @Override
        public void onDisconnect(Peer peer, PeerServer server) {

        }
    };
}
