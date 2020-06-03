package org.tdf.sunflower.facade;

import lombok.Getter;
import lombok.Setter;
import org.tdf.common.event.EventBus;
import org.tdf.common.store.Store;
import org.tdf.common.trie.Trie;
import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.state.*;
import org.tdf.sunflower.types.Block;

import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.function.Function;


@Getter
@Setter
public abstract class AbstractConsensusEngine implements ConsensusEngine {
    // contract storage trie
    private Trie<byte[], byte[]> contractStorageTrie;

    // a map between hash and wasm byte code
    private Store<byte[], byte[]> contractCodeStore;

    private SecretStore secretStore;

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

    // sunflowerRepository will be injected before init() called
    private SunflowerRepository sunflowerRepository;

    // sub class should set confirmedBlocksProvider explicitly when init() called
    private ConfirmedBlocksProvider confirmedBlocksProvider = x -> x;

    // sub class should set peer server listener explicitly when init() called
    private PeerServerListener peerServerListener;

    // set before init()
    private Function<AbstractConsensusEngine, StateTrie<HexBytes, Account>> stateTrieProvider;

    public AbstractConsensusEngine() {
    }

    public List<Account> getGenesisStates() {
        return Collections.emptyList();
    }

    public List<PreBuiltContract> getPreBuiltContracts() {
        return Collections.emptyList();
    }

    public List<Bios> getBios() {
        return Collections.emptyList();
    }

    protected void initStateTrie() {
        StateTrie<HexBytes, Account> trie = stateTrieProvider.apply(this);
        setAccountTrie(trie);
        Block b = getGenesisBlock();
        b.setStateRoot(trie.getGenesisRoot());
        setGenesisBlock(b);
    }

    public static final AbstractConsensusEngine NONE = new AbstractConsensusEngine() {
        @Override
        public void init(Properties properties) {
            if (getTransactionPool() == null)
                throw new RuntimeException("transaction pool not injected");
            if (getSunflowerRepository() == null)
                throw new RuntimeException("consortium repository not injected");
            if (getEventBus() == null) {
                throw new RuntimeException("event bus not injected");
            }

            setMiner(Miner.NONE);
            setValidator(Validator.NONE);
            setGenesisBlock(new Block());
            setPeerServerListener(PeerServerListener.NONE);
            initStateTrie();
        }

        @Override
        public String getName() {
            return "none";
        }
    };
}
