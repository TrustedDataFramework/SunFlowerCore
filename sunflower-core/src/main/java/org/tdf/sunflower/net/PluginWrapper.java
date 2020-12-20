package org.tdf.sunflower.net;

import org.tdf.sunflower.facade.PeerServerListener;
import org.tdf.sunflower.proto.Code;

// wrap listener as plugin
public class PluginWrapper implements Plugin {
    private final PeerServerListener listener;

    PluginWrapper(PeerServerListener listener) {
        this.listener = listener;
    }

    @Override
    public void onMessage(ContextImpl context, PeerServerImpl server) {
        if (context.message.getCode().equals(Code.ANOTHER)) {
            listener.onMessage(context, server);
        }
    }

    @Override
    public void onStart(PeerServerImpl server) {
        listener.onStart(server);
    }

    @Override
    public void onNewPeer(PeerImpl peer, PeerServerImpl server) {
        listener.onNewPeer(peer, server);
    }

    @Override
    public void onDisconnect(PeerImpl peer, PeerServerImpl server) {
        listener.onDisconnect(peer, server);
    }

    @Override
    public void onStop(PeerServerImpl server) {

    }
}
