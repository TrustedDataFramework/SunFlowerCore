package org.tdf.sunflower.facade;

import lombok.Getter;
import lombok.Setter;
import org.tdf.common.event.EventBus;
import org.tdf.common.store.Store;
import org.tdf.common.trie.Trie;
import org.tdf.common.util.HexBytes;
import org.tdf.crypto.keystore.Keystore;
import org.tdf.sunflower.db.DatabaseStoreFactory;
import org.tdf.sunflower.exception.ConsensusEngineInitException;
import org.tdf.sunflower.state.*;
import org.tdf.sunflower.types.Block;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Getter
@Setter
public abstract class AbstractConsensusEngine implements ConsensusEngine {
    static final Properties NONE_PROPERTIES;

    static {
        NONE_PROPERTIES = new Properties();
        NONE_PROPERTIES.put("name", "none");
    }


    // contract storage trie
    private Trie<byte[], byte[]> contractStorageTrie;

    // a map between hash and wasm byte code
    private Store<byte[], byte[]> contractCodeStore;

    private Keystore keystore;

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

    // sunflowerRepository will be injected before init() called
    private SunflowerRepository sunflowerRepository;

    // sub class should set confirmedBlocksProvider explicitly when init() called
    private ConfirmedBlocksProvider confirmedBlocksProvider = x -> x;

    // sub class should set peer server listener explicitly when init() called
    private PeerServerListener peerServerListener;

    private String name;

    public AbstractConsensusEngine(){
    }

    protected void initStates(Block genesis, List<Account> alloc, List<PreBuiltContract> preBuiltContractList, List<Bios> biosList){
        AccountUpdater updater = new AccountUpdater(
                alloc.stream().collect(Collectors.toMap(Account::getAddress, Function.identity())),
                getContractCodeStore(), getContractStorageTrie(),
                preBuiltContractList, biosList
        );

        AccountTrie trie = new AccountTrie(
                updater, getDatabaseStoreFactory(),
                getContractCodeStore(), getContractStorageTrie()
        );

        setAccountTrie(trie);
        genesis.setStateRoot(trie.getGenesisRoot());
        setGenesisBlock(genesis);
    }

    public static final AbstractConsensusEngine NONE = new AbstractConsensusEngine() {
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
            setPeerServerListener(PeerServerListener.NONE);
            AccountUpdater updater = new AccountUpdater(
                    Collections.emptyMap(),
                    getContractCodeStore(),
                    getContractStorageTrie(),
                    Collections.emptyList(),
                    Collections.emptyList()
            );
            setAccountTrie(new AccountTrie(updater, getDatabaseStoreFactory(), getContractCodeStore(), getContractStorageTrie()));
        }

        @Override
        public String getName() {
            return "none";
        }
    };
}
