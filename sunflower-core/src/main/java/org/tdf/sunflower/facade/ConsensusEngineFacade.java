package org.tdf.sunflower.facade;

import org.springframework.context.ApplicationContext;
import org.tdf.common.event.EventBus;
import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.db.DatabaseStoreFactory;
import org.tdf.sunflower.exception.ConsensusEngineInitException;
import org.tdf.sunflower.state.Account;
import org.tdf.sunflower.state.StateTrie;
import org.tdf.sunflower.types.Block;

import java.util.Properties;

public interface ConsensusEngineFacade {
    // the below getters shouldn't return null after initialized
    Miner getMiner();
    Validator getValidator();
    StateTrie<HexBytes, Account> getAccountTrie();
    Block getGenesisBlock();
    ConfirmedBlocksProvider getConfirmedBlocksProvider();
    PeerServerListener getPeerServerListener();

    // the below methods are called before init() to inject dependencies
    void setApplicationContext(ApplicationContext context);

    // inject configurations, throw exception if configuration is invalid
    void init(Properties properties) throws ConsensusEngineInitException;
}
