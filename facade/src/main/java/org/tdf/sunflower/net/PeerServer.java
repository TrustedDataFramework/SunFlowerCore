package org.tdf.sunflower.net;

import org.tdf.sunflower.facade.PeerServerListener;

import java.util.Collections;
import java.util.List;
import java.util.Properties;

public interface PeerServer {
    PeerServer NONE = new PeerServer() {
        @Override
        public void dial(Peer peer, byte[] message) {

        }

        @Override
        public void broadcast(byte[] message) {

        }

        @Override
        public List<Peer> getPeers() {
            return Collections.emptyList();
        }

        @Override
        public List<Peer> getBootStraps() {
            return Collections.emptyList();
        }

        @Override
        public void addListeners(PeerServerListener... peerServerListeners) {

        }

        @Override
        public void start() {

        }

        @Override
        public void stop() {

        }

        @Override
        public void init(Properties properties) {

        }

        @Override
        public Peer getSelf() {
            return Peer.NONE;
        }

        @Override
        public boolean isFull() {
            return false;
        }
    };

    // dial a peer with a message
    void dial(Peer peer, byte[] message);

    // broadcast a message to all the peers
    void broadcast(byte[] message);

    // get all peers had been connected
    List<Peer> getPeers();

    // get all bootstraps
    List<Peer> getBootStraps();

    boolean isFull();

    void addListeners(PeerServerListener... peerServerListeners);

    void start();

    void stop();

    void init(Properties properties);

    Peer getSelf();
}
