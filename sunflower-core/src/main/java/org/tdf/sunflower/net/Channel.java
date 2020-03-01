package org.tdf.sunflower.net;

import org.tdf.sunflower.proto.Message;

import java.util.Optional;

// channel for message transports
interface Channel{
    // write message to channel
    void write(Message message);

    // close the channel
    default void close(){
        close("");
    }

    // close the channel
    void close(String reason);

    default boolean isAlive(){
        return !isClosed();
    }

    // check whether the channel is closed
    boolean isClosed();

    // notify listeners new message received
    void message(Message message);

    // notify listeners error
    void error(Throwable throwable);

    Optional<PeerImpl> getRemote();

    // bind listener to the channel
    void addListeners(ChannelListener... listeners);

    void setMessageBuilder(MessageBuilder builder);
}
