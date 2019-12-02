package org.tdf.consortium.net;

import org.tdf.common.HexBytes;
import org.tdf.common.Peer;
import org.tdf.common.PeerServer;
import org.tdf.common.PeerServerListener;
import org.tdf.exception.PeerServerLoadException;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

// none is a trivial peer server used in spring tests
public class None implements PeerServer {
    @Override
    public void dial(Peer peer, byte[] message) {

    }

    @Override
    public void broadcast(byte[] message) {

    }

    @Override
    public List<Peer> getPeers() {
        return new ArrayList<>();
    }

    @Override
    public List<Peer> getBootStraps() {
        return new ArrayList<>();
    }

    @Override
    public void use(PeerServerListener... peerServerListeners) {

    }

    @Override
    public void start() {

    }

    @Override
    public void load(Properties properties) throws PeerServerLoadException {

    }

    @Override
    public Peer getSelf() {
        return new Peer() {
            @Override
            public String getHost() {
                return "";
            }

            @Override
            public int getPort() {
                return 0;
            }

            @Override
            public HexBytes getID() {
                return new HexBytes();
            }

            @Override
            public String encodeURI() {
                return "";
            }
        };
    }
}
