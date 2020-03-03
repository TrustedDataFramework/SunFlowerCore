package org.tdf.sunflower.net;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.tdf.sunflower.proto.Message;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;


// communicating channel with peer
@Slf4j(topic = "net")
@RequiredArgsConstructor
public class ProtoChannel implements Channel {
    public interface ChannelOut {
        void write(Message message);

        void close();
    }

    private volatile boolean closed;
    private PeerImpl remote;
    private ChannelOut out;
    private volatile boolean pinged;
    private List<ChannelListener> listeners = new CopyOnWriteArrayList<>();

    private final MessageBuilder messageBuilder;

    public void setOut(ChannelOut out) {
        this.out = out;
    }

    @Override
    public void message(Message message) {
        if (closed) return;
        handlePing(message);
        if (listeners == null) return;
        for (ChannelListener listener : listeners) {
            if (closed) return;
            listener.onMessage(message, this);
        }
    }

    private void handlePing(Message message) {
        if (pinged) return;
        Optional<PeerImpl> o = PeerImpl.parse(message.getRemotePeer(), messageBuilder.getSelf());
        if (!o.isPresent()) {
            close("invalid peer " + message.getRemotePeer());
            return;
        }
        pinged = true;
        remote = o.get();
        if (listeners == null) return;
        for (ChannelListener listener : listeners) {
            if (closed) return;
            listener.onConnect(remote, this);
        }
    }

    @Override
    public void error(Throwable throwable) {
        if (closed || listeners == null) return;
        for (ChannelListener listener : listeners) {
            if (closed) return;
            listener.onError(throwable, this);
        }
    }


    public void close(String reason) {
        if (closed) return;
        closed = true;
        if (reason != null && !reason.isEmpty()) {
            out.write(messageBuilder.buildDisconnect(reason));
            log.error("close channel to " + remote + " reason is " + reason);
        }
        if (listeners == null) return;
        listeners.forEach(l -> l.onClose(this));
        listeners = null;
        try {
            out.close();
        } catch (Exception ignore) {
        }
    }

    public void write(Message message) {
        if (closed) {
            log.error("the channel is closed");
            return;
        }
        try {
            out.write(message);
        } catch (Throwable e) {
            e.printStackTrace();
            log.error(e.getMessage());
            if (listeners == null) return;
            error(e);
        }
    }

    public Optional<PeerImpl> getRemote() {
        return Optional.ofNullable(remote);
    }

    public boolean isClosed() {
        return closed;
    }

    @Override
    public void addListeners(ChannelListener... listeners) {
        if (listeners == null) return;
        this.listeners.addAll(Arrays.asList(listeners));
    }
}
