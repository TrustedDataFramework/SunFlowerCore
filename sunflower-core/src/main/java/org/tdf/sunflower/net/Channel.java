package org.tdf.sunflower.net;

import org.tdf.sunflower.proto.Message;

import java.io.Closeable;
import java.util.Optional;

// channel for message transports
interface Channel extends Closeable {
    // write message to channel
    void write(Message message);

    // close the channel
    void close();

    // check whether the channel is closed
    boolean isClosed();

    // notify listeners new message received
    void message(Message message);

    // notify listeners error
    void error(Throwable throwable);

    void setOut(ChannelOut out);

    interface ChannelListener {
        // triggered when channel is open, only once in the life cycle of the channel
        void onConnect(PeerImpl remote, Channel channel);

        // when new message received
        void onMessage(Message message, Channel channel);

        // when error occurred
        void onError(Throwable throwable, Channel channel);

        // when the channel been closed
        void onClose(Channel channel);
    }

    interface ChannelOut {
        void write(Message message);
        void close();
    }

    Optional<PeerImpl> getRemote();

    // bind listener to the channel
    void addListener(ChannelListener... listeners);
}
