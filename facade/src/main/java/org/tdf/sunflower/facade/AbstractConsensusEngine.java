package org.tdf.sunflower.facade;

import lombok.Getter;
import lombok.Setter;
import org.tdf.common.event.EventBus;
import org.tdf.common.store.Store;
import org.tdf.common.trie.Trie;
import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.state.Account;
import org.tdf.sunflower.state.BuiltinContract;
import org.tdf.sunflower.state.StateTrie;
import org.tdf.sunflower.types.Block;
import org.tdf.sunflower.types.ConsensusConfig;

import java.util.Collections;
import java.util.List;


@Getter
@Setter
public abstract class AbstractConsensusEngine implements ConsensusEngine {
    public static final AbstractConsensusEngine NONE = new AbstractConsensusEngine() {
        @Override
        public void init(ConsensusConfig config) {
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
        }

        @Override
        public String getName() {
            return "none";
        }
    };

    // contract storage trie
    private Trie<HexBytes, HexBytes> contractStorageTrie;
    // a map between hash and wasm byte code
    private Store<HexBytes, HexBytes> contractCodeStore;
    // sub class should set miner explicitly when init() called
    private Miner miner;
    // sub class should set validator explicitly when init() called
    private Validator validator;
    // account stateTrie will be injected before init() called
    private StateTrie<HexBytes, Account> accountTrie;
    // sub class should set genesis block explicitly when init() called
    private Block genesisBlock;
    // event bus will be injected before init() called
    private EventBus eventBus;
    // transaction pool will be injected before init() called
    private TransactionPool transactionPool;
    // sunflowerRepository will be injected before init() called
    private IRepositoryService sunflowerRepository;

    // sub class should set peer server listener explicitly when init() called
    private PeerServerListener peerServerListener = PeerServerListener.NONE;


    public AbstractConsensusEngine() {
    }

    public List<Account> getAlloc() {
        return Collections.emptyList();
    }

    public List<BuiltinContract> getBuiltins() {
        return Collections.emptyList();
    }

    public List<BuiltinContract> getBios() {
        return Collections.emptyList();
    }
}
