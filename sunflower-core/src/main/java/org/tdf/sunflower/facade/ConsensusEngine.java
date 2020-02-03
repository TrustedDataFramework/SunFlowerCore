package org.tdf.sunflower.facade;

import lombok.Getter;
import lombok.Setter;
import org.tdf.common.event.EventBus;
import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.db.DatabaseStoreFactory;
import org.tdf.sunflower.exception.ConsensusEngineInitException;
import org.tdf.sunflower.state.Account;
import org.tdf.sunflower.state.AccountTrie;
import org.tdf.sunflower.state.AccountUpdater;
import org.tdf.sunflower.state.StateTrie;
import org.tdf.sunflower.types.Block;

import java.util.Collections;
import java.util.Properties;

@Getter
@Setter
public abstract class ConsensusEngine implements ConsensusEngineFacade{
    // sub class should set miner explicitly when init() called
    private Miner miner;

    // sub class should set validator explicitly when init() called
    private Validator validator;

    // sub class should set account stateTrie explicitly when init() called
    private StateTrie<HexBytes, Account> accountTrie;

    // sub class should set genesis block explicitly when init() called
    private Block genesisBlock;

    // event bus will be injected before init() called
    private EventBus eventBus;

    // transaction pool will be injected before init() called
    private TransactionPool transactionPool;

    // database store factory will be injected before init() called
    private DatabaseStoreFactory databaseStoreFactory;

    // consortiumRepository will be injected before init() called
    private SunflowerRepository sunflowerRepository;

    // sub class should set hash policy explicitly when init() called
    private HashPolicy hashPolicy;

    // sub class should set confirmedBlocksProvider explicitly when init() called
    private ConfirmedBlocksProvider confirmedBlocksProvider;

    // sub class should set peer server listener explicitly when init() called
    private PeerServerListener peerServerListener;

    public static final ConsensusEngine NONE = new ConsensusEngine() {
        @Override
        public void init(Properties properties) throws ConsensusEngineInitException {
            if(getTransactionPool() == null)
                throw new ConsensusEngineInitException("transaction pool not injected");
            if(getSunflowerRepository() == null)
                throw new ConsensusEngineInitException("consortium repository not injected");
            if(getEventBus() == null){
                throw new ConsensusEngineInitException("event bus not injected");
            }
            if(getDatabaseStoreFactory() == null)
                throw new ConsensusEngineInitException("database factory not injected");
            setMiner(Miner.NONE);
            setValidator(Validator.NONE);
            setGenesisBlock(new Block());
            setHashPolicy(HashPolicy.NONE);
            setConfirmedBlocksProvider(unconfirmed -> unconfirmed);
            setPeerServerListener(PeerServerListener.NONE);
            AccountUpdater updater = new AccountUpdater(Collections.emptyMap());
            AccountTrie trie = new AccountTrie(updater, getDatabaseStoreFactory());
            setAccountTrie(trie);
        }
    };
}
