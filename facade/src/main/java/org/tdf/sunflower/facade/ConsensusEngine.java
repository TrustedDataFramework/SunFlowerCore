package org.tdf.sunflower.facade;

import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.state.Account;
import org.tdf.sunflower.state.StateTrie;
import org.tdf.sunflower.types.Block;
import org.tdf.sunflower.types.ConsensusConfig;
import org.tdf.sunflower.types.ValidateResult;

import java.util.*;

public interface ConsensusEngine {

    // the below getters shouldn't return null after initialized
    Miner getMiner();

    Validator getValidator();

    StateTrie<HexBytes, Account> getAccountTrie();

    Block getGenesisBlock();

    ConfirmedBlocksProvider getConfirmedBlocksProvider();

    PeerServerListener getPeerServerListener();

    // inject configurations, throw exception if configuration is invalid
    void init(ConsensusConfig config);

    String getName();

    default ValidateResult validateHeader(String header) {
        return ValidateResult.success();
    }

    default int getChainId() {
        return 102;
    }
}
