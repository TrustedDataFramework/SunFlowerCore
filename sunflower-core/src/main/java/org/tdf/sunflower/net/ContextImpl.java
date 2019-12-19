package org.tdf.sunflower.net;

import lombok.Builder;
import org.tdf.sunflower.proto.Message;

import java.util.Collection;
import java.util.Collections;

@Builder
public class ContextImpl implements Context {
    boolean relayed;
    boolean exited;
    boolean disconnected;
    boolean blocked;
    PeerImpl remote;
    Message message;
    Channel channel;
    MessageBuilder builder;
    Client client;

    @Override
    public void exit() {
        exited = true;
    }

    @Override
    public void disconnect() {
        if (exited || blocked || disconnected) return;
        disconnected = true;
        client.peersCache.remove(remote, " disconnect to " + remote + " by listener");
    }

    @Override
    public void block() {
        if (exited || blocked) return;
        blocked = true;
        disconnected = true;
        client.peersCache.block(remote);
    }

    @Override
    public void keep() {
        if (exited || blocked || disconnected) return;
        client.peersCache.keep(remote, channel);
    }

    @Override
    public void response(byte[] message) {
        response(Collections.singleton(message));
    }

    @Override
    public void response(Collection<byte[]> messages) {
        if (exited || blocked || disconnected) return;
        for (byte[] msg : messages) {
            channel.write(builder.buildAnother(msg));
        }
    }

    @Override
    public void relay() {
        if (exited || blocked || disconnected || relayed || message.getTtl() == 0) return;
        relayed = true;
        client.relay(message, remote);
    }

    @Override
    public byte[] getMessage() {
        return message.getBody().toByteArray();
    }

    @Override
    public Peer getRemote() {
        return remote;
    }
}
