package org.tdf.sunflower.net;

import java.io.Closeable;
import java.util.Optional;
import java.util.function.Consumer;

// transport layer where p2p network builds on
public interface NetLayer extends Closeable {

    // start listening
    void start();

    // register channel incoming handler
    void onChannelIncoming(Consumer<Channel> channelHandler);

    // create a channel as a client
    Optional<Channel> createChannel(String host, int port, Channel.ChannelListener... listeners);
}
