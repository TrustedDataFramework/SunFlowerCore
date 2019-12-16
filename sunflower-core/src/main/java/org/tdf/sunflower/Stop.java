package org.tdf.sunflower;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tdf.common.Miner;
import org.tdf.common.PeerServer;
import org.tdf.sunflower.db.DatabaseStoreFactory;

import javax.annotation.PreDestroy;

// resource cleaner
@Component
@Slf4j
public class Stop {
    @Autowired
    private Miner miner;

    @Autowired
    private PeerServer peerServer;

    @Autowired
    private DatabaseStoreFactory factory;

    public Stop(){

    }

    @PreDestroy
    public void onDestroy() throws Exception{
        log.info("clean resources before exit program...");
        miner.stop();
        peerServer.stop();
        factory.cleanup();
    }
}
