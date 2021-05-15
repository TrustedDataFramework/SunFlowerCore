package org.tdf.sunflower.net;

import lombok.extern.slf4j.Slf4j;
import org.tdf.sunflower.proto.Code;
import org.tdf.sunflower.proto.Message;

import java.net.URI;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Consumer;

@Slf4j(topic = "net")
public class Client implements ChannelListener {
    private final PeerServerConfig config;
    private final NetLayer netLayer;
    private final PeerImpl self;
    private final MessageBuilder builder;
    MessageBuilder messageBuilder;
    PeersCache peersCache;
    // listener for channel event
    private ChannelListener listener = ChannelListener.NONE;

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
        this.self = self;
        this.builder = new MessageBuilder(self, config);
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
        getChannel(host, port, listener, new ChannelListener() {
            @Override
            public void onConnect(PeerImpl remote, Channel channel) {
                connectionConsumer.accept(remote);
                addPeer(remote, channel);
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
        // cannot create channel connect to your self
        if (peer.equals(self)) return Optional.empty();
        Optional<Channel> ch = peersCache
            .getChannel(peer.getID());
        if (ch.isPresent()) return ch;
        ch = netLayer
            .createChannel(peer.getHost(), peer.getPort(), this, listener)
            .filter(Channel::isAlive);
        return ch;
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
            .findAny();
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
        addPeer(remote, channel);
    }

    private void addPeer(PeerImpl remote, Channel channel) {
        if (remote.equals(self)) {
            channel.close("close channel connect to self");
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
        throwable.printStackTrace();
        channel.getRemote()
            .ifPresent(x -> {
                peersCache.half(x);
                log.error("error found decrease the score of peer " + x + " " + throwable.getMessage());
            });

    }

    @Override
    public void onClose(Channel channel) {
        Optional<PeerImpl> remote = channel.getRemote();
        if (!remote.isPresent()) return;
//        log.error("close channel to " + remote.get());
        peersCache.remove(remote.get().getID(), " channel closed");
    }

    void relay(Message message, PeerImpl receivedFrom) {
        peersCache.getChannels()
            .filter(x -> x.getRemote().map(p -> !p.equals(receivedFrom)).orElse(false))
            .forEach(c -> {
                if (message.getCode() == Code.ANOTHER) {
                    byte[] msg =
                        message.getBody().toByteArray();
                    c.write(builder.buildMessage(Code.ANOTHER, message.getTtl() - 1, msg));
                    return;
                }
                c.write(messageBuilder.buildRelay(message));
            });
    }
}
