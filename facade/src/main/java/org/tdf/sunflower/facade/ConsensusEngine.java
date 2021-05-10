package org.tdf.sunflower.facade;

import org.tdf.sunflower.types.Block;
import org.tdf.sunflower.types.ConsensusConfig;

public interface ConsensusEngine {

    // the below getters shouldn't return null after initialized
    Miner getMiner();

    Validator getValidator();

    Block getGenesisBlock();

    PeerServerListener getPeerServerListener();

    // inject configurations, throw exception if configuration is invalid
    void init(ConsensusConfig config);

    String getName();

    default int getChainId() {
        return 102;
    }
}
