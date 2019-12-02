package org.tdf.common;

// Peer is a p2p node could be connected
public interface Peer {
    // get the host name of remote peer
    String getHost();

    // get the server port of remote
    int getPort();

    // the id is typically a ed25519 public key
    HexBytes getID();

    // encode the remote peer as uri
    String encodeURI();
}
