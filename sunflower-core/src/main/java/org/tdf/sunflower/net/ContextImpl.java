package org.tdf.sunflower.net;

import org.tdf.sunflower.proto.Code;
import org.tdf.sunflower.proto.Message;

import java.util.Collection;
import java.util.Collections;

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

    private byte[] decrypted;

    ContextImpl(boolean relayed, boolean exited, boolean disconnected, boolean blocked, PeerImpl remote, Message message, Channel channel, MessageBuilder builder, Client client, byte[] decrypted) {
        this.relayed = relayed;
        this.exited = exited;
        this.disconnected = disconnected;
        this.blocked = blocked;
        this.remote = remote;
        this.message = message;
        this.channel = channel;
        this.builder = builder;
        this.client = client;
        this.decrypted = decrypted;
    }

    public static ContextImplBuilder builder() {
        return new ContextImplBuilder();
    }

    @Override
    public void exit() {
        exited = true;
    }

    @Override
    public void disconnect() {
        if (exited || blocked || disconnected) return;
        disconnected = true;
        client.peersCache.remove(remote.getID(), " disconnect to " + remote + " by listener");
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
            builder.buildAnother(msg, 1, remote)
                .forEach(channel::write);
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
        if (message.getCode() == Code.ANOTHER) {
            if (decrypted != null)
                return decrypted;
            decrypted =
                message.getBody().toByteArray();
            return decrypted;
        }
        return message.getBody().toByteArray();
    }

    @Override
    public Peer getRemote() {
        return remote;
    }

    public static class ContextImplBuilder {
        private boolean relayed;
        private boolean exited;
        private boolean disconnected;
        private boolean blocked;
        private PeerImpl remote;
        private Message message;
        private Channel channel;
        private MessageBuilder builder;
        private Client client;
        private byte[] decrypted;

        ContextImplBuilder() {
        }

        public ContextImplBuilder relayed(boolean relayed) {
            this.relayed = relayed;
            return this;
        }

        public ContextImplBuilder exited(boolean exited) {
            this.exited = exited;
            return this;
        }

        public ContextImplBuilder disconnected(boolean disconnected) {
            this.disconnected = disconnected;
            return this;
        }

        public ContextImplBuilder blocked(boolean blocked) {
            this.blocked = blocked;
            return this;
        }

        public ContextImplBuilder remote(PeerImpl remote) {
            this.remote = remote;
            return this;
        }

        public ContextImplBuilder message(Message message) {
            this.message = message;
            return this;
        }

        public ContextImplBuilder channel(Channel channel) {
            this.channel = channel;
            return this;
        }

        public ContextImplBuilder builder(MessageBuilder builder) {
            this.builder = builder;
            return this;
        }

        public ContextImplBuilder client(Client client) {
            this.client = client;
            return this;
        }

        public ContextImplBuilder decrypted(byte[] decrypted) {
            this.decrypted = decrypted;
            return this;
        }

        public ContextImpl build() {
            return new ContextImpl(relayed, exited, disconnected, blocked, remote, message, channel, builder, client, decrypted);
        }

        public String toString() {
            return "ContextImpl.ContextImplBuilder(relayed=" + this.relayed + ", exited=" + this.exited + ", disconnected=" + this.disconnected + ", blocked=" + this.blocked + ", remote=" + this.remote + ", message=" + this.message + ", channel=" + this.channel + ", builder=" + this.builder + ", client=" + this.client + ", decrypted=" + java.util.Arrays.toString(this.decrypted) + ")";
        }
    }
}
