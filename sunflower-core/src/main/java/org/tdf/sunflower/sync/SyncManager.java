package org.tdf.sunflower.sync;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.tdf.sunflower.facade.PeerServerListener;
import org.tdf.sunflower.net.Context;
import org.tdf.sunflower.net.Peer;
import org.tdf.sunflower.net.PeerServer;

import javax.annotation.PostConstruct;

@Component
@RequiredArgsConstructor
public class SyncManager implements PeerServerListener {
    private final PeerServer peerServer;

    @PostConstruct
    public void init(){
        peerServer.addListeners(this);
    }

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
}
