package org.tdf.sunflower;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tdf.common.Miner;
import org.tdf.common.PeerServer;
import org.tdf.sunflower.db.DatabaseStoreFactory;

import javax.annotation.PreDestroy;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;

import static org.tdf.sunflower.ApplicationConstants.SHUTDOWN_SIGNAL;

// resource cleaner
@Component
@Slf4j
public class SignalHandler {
    private static final BlockingQueue<Integer> SIGNALS = new ArrayBlockingQueue<>(1);

    @Autowired
    private Miner miner;

    @Autowired
    private PeerServer peerServer;

    @Autowired
    private DatabaseStoreFactory factory;

    public void emit(int signal){
        SIGNALS.add(signal);
    }

    public SignalHandler(){
        // waiting for shutdown signals
        log.info("listening for shutdown signal to close application...");
        Executors.newSingleThreadExecutor()
                .execute(() -> {
                    while (true) {
                        int signal;
                        try {
                            signal = SIGNALS.take();
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                        switch (signal) {
                            case SHUTDOWN_SIGNAL:
                                log.info("shutdown signal received, closing application...");
                                System.exit(0);
                        }
                    }
                });
    }

    @PreDestroy
    public void onDestroy() throws Exception{
        log.info("clean resources before exit program...");
        miner.stop();
        peerServer.stop();
        factory.cleanup();
    }
}
