package org.tdf.sunflower.net;

import lombok.extern.slf4j.Slf4j;
import org.tdf.sunflower.proto.Message;

import java.net.URI;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Consumer;

@Slf4j
public class Client implements ChannelListener {
    // listener for channel event
    private ChannelListener listener = ChannelListener.NONE;
    private PeerServerConfig config;
    MessageBuilder messageBuilder;
    PeersCache peersCache;
    private NetLayer netLayer;

    public Client(
            PeerImpl self,
            PeerServerConfig config,
            MessageBuilder messageBuilder,
            NetLayer netLayer
    ) {
        this.peersCache = new PeersCache(self, config);
        this.config = config;
        this.messageBuilder = messageBuilder;
        this.netLayer = netLayer;
    }

    Client withListener(ChannelListener listener) {
        this.listener = listener;
        return this;
    }

    void broadcast(Message message) {
        peersCache.getChannels().forEach(ch -> ch.write(message));
    }

    public void dial(Peer peer, Message message) {
        getChannel(peer).ifPresent(x -> x.write(message));
    }

    public void dial(String host, int port, Message message) {
        getChannel(host, port, this, listener)
                .ifPresent(ch -> ch.write(message));
    }

    void bootstrap(Collection<URI> uris) {
        for (URI uri : uris) {
            connect(uri.getHost(), uri.getPort(), (peer) -> peersCache.bootstraps.put(peer, true));
        }
    }

    void trust(Collection<URI> trusted) {
        for (URI uri : trusted) {
            connect(uri.getHost(), uri.getPort(), (peer) -> peersCache.trusted.put(peer, true));
        }
    }

    // functional interface for connect to bootstrap and trusted peer
    // consumer may be called more than once
    // usually called when server starts
    private void connect(String host, int port, Consumer<PeerImpl> connectionConsumer) {
        getChannel(host, port, this, listener, new ChannelListener() {
            @Override
            public void onConnect(PeerImpl remote, Channel channel) {
                connectionConsumer.accept(remote);
            }

            @Override
            public void onMessage(Message message, Channel channel) {

            }

            @Override
            public void onError(Throwable throwable, Channel channel) {

            }

            @Override
            public void onClose(Channel channel) {

            }
        })
        .flatMap(Channel::getRemote)
        // if the connection had already created, onConnect will not triggered
        // but the peer will be handled here
        .ifPresent(connectionConsumer);
    }

    // try to get channel from cache, if channel not exists in cache,
    // create from net layer
    private Optional<Channel> getChannel(Peer peer) {
        Optional<Channel> ch = peersCache
                .getChannel(peer.getID())
                .filter(Channel::isAlive);
        if(ch.isPresent()) return ch;
        return netLayer
                .createChannel(peer.getHost(), peer.getPort(), this, listener)
                .filter(Channel::isAlive);
    }

    // try to get channel from cache, if channel not exists in cache,
    // create from net layer
    private Optional<Channel> getChannel(String host, int port, ChannelListener... listeners) {
        Optional<Channel> ch = peersCache.getChannels()
                .filter(
                        x -> x.getRemote().map(
                                p -> p.getHost().equals(host) && p.getPort() == port
                        ).orElse(false)
                )
                .findAny()
                .filter(Channel::isAlive);
        if (ch.isPresent()) return ch;
        ch = netLayer
                .createChannel(host, port, listeners)
                .filter(Channel::isAlive)
        ;
        ch.ifPresent(c -> c.write(messageBuilder.buildPing()));
        return ch;
    }

    @Override
    public void onConnect(PeerImpl remote, Channel channel) {
        if (!config.isEnableDiscovery() &&
                !peersCache.bootstraps.containsKey(remote) &&
                !peersCache.trusted.containsKey(remote)
        ) {
            channel.close("discovery is not enabled accept bootstraps and trusted only");
            return;
        }
        Optional<Channel> o = peersCache.getChannel(remote);
        if (o.map(Channel::isAlive).orElse(false)) {
            log.error("the channel to " + remote + " had been created");
            return;
        }
        peersCache.keep(remote, channel);
    }

    @Override
    public void onMessage(Message message, Channel channel) {
    }

    @Override
    public void onError(Throwable throwable, Channel channel) {
        channel.getRemote()
                .filter(x -> !peersCache.hasBlocked(x))
                .ifPresent(x -> peersCache.half(x));
        log.error("error found decrease the score of peer " + channel.getRemote() + " " + throwable.getMessage());
    }

    @Override
    public void onClose(Channel channel) {
        Optional<PeerImpl> remote = channel.getRemote();
        if (!remote.isPresent()) return;
//        log.error("close channel to " + remote.get());
        peersCache.remove(remote.get(), " channel closed");
    }

    void relay(Message message, PeerImpl receivedFrom) {
        peersCache.getChannels()
                .filter(x -> x.getRemote().map(p -> !p.equals(receivedFrom)).orElse(false))
                .forEach(c -> c.write(messageBuilder.buildRelay(message)));
    }
}
