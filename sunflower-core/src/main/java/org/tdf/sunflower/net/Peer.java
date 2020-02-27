package org.tdf.sunflower.net;

import org.tdf.common.util.HexBytes;

// Peer is a p2p node could be connected
public interface Peer {
    // get the host name of remote peer
    String getHost();

    // get the server port of remote
    int getPort();

    // the id is typically an ecc public key
    HexBytes getID();

    // encode the remote peer as uri
    String encodeURI();

    Peer NONE = new Peer() {
        @Override
        public String getHost() {
            return null;
        }

        @Override
        public int getPort() {
            return 0;
        }

        @Override
        public HexBytes getID() {
            return null;
        }

        @Override
        public String encodeURI() {
            return null;
        }
    };
}
