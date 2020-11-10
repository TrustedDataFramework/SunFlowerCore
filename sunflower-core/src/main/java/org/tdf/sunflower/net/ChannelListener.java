package org.tdf.sunflower.net;

import org.tdf.sunflower.proto.Message;

public interface ChannelListener {
    ChannelListener NONE = new ChannelListener() {
        @Override
        public void onConnect(PeerImpl remote, Channel channel) {

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
    };

    // triggered when channel is open, only once in the life cycle of the channel
    void onConnect(PeerImpl remote, Channel channel);

    // when new message received
    void onMessage(Message message, Channel channel);

    // when error occurred
    void onError(Throwable throwable, Channel channel);

    // when the channel been closed
    void onClose(Channel channel);
}
