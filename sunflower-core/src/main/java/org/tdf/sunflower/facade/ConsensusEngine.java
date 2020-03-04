package org.tdf.sunflower.facade;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationContext;
import org.tdf.common.event.EventBus;
import org.tdf.common.store.Store;
import org.tdf.common.trie.Trie;
import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.db.DatabaseStoreFactory;
import org.tdf.sunflower.exception.ConsensusEngineInitException;
import org.tdf.sunflower.state.Account;
import org.tdf.sunflower.state.StateTrie;
import org.tdf.sunflower.types.Block;

import java.util.Properties;

@Getter
public abstract class ConsensusEngine implements ConsensusEngineFacade{
    protected ApplicationContext applicationContext;

    private Trie<byte[], byte[]> contractStorageTrie;
    private Store<byte[], byte[]> contractCodeStore;

    public void setApplicationContext(ApplicationContext context){
        this.applicationContext = context;
        this.eventBus = context.getBean(EventBus.class);
        this.transactionPool = context.getBean(TransactionPool.class);
        this.databaseStoreFactory = context.getBean(DatabaseStoreFactory.class);
        this.sunflowerRepository = context.getBean(SunflowerRepository.class);
        this.contractStorageTrie = context.getBean("contractStorageTrie", Trie.class);
        this.contractCodeStore = context.getBean("contractCodeStore", Store.class);
    }

    // sub class should set miner explicitly when init() called
    @Setter(AccessLevel.PROTECTED)
    private Miner miner;

    // sub class should set validator explicitly when init() called
    @Setter(AccessLevel.PROTECTED)
    private Validator validator;

    // sub class should set account stateTrie explicitly when init() called
    @Setter(AccessLevel.PROTECTED)
    private StateTrie<HexBytes, Account> accountTrie;

    // sub class should set genesis block explicitly when init() called
    @Setter(AccessLevel.PROTECTED)
    private Block genesisBlock;

    // event bus will be injected before init() called
    private EventBus eventBus;

    // transaction pool will be injected before init() called
    private TransactionPool transactionPool;

    // database store factory will be injected before init() called
    private DatabaseStoreFactory databaseStoreFactory;

    // consortiumRepository will be injected before init() called
    private SunflowerRepository sunflowerRepository;

    // sub class should set confirmedBlocksProvider explicitly when init() called
    @Setter(AccessLevel.PROTECTED)
    private ConfirmedBlocksProvider confirmedBlocksProvider;

    // sub class should set peer server listener explicitly when init() called
    @Setter(AccessLevel.PROTECTED)
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
        }
    };
}
