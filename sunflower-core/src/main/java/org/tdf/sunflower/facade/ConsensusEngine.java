package org.tdf.sunflower.facade;

import lombok.Getter;
import lombok.Setter;
import org.tdf.sunflower.exception.ConsensusEngineInitException;
import org.tdf.sunflower.types.Block;

import java.util.Properties;

@Getter
@Setter
public abstract class ConsensusEngine {
    // sub class should set miner explicitly when init() called
    private Miner miner;

    // sub class should set validator explicitly when init() called
    private Validator validator;

    // sub class should set stateRepository explicitly when init() called
    private StateRepository stateRepository;

    // sub class should set genesis block explicitly when init() called
    private Block genesisBlock;

    // transaction pool will be injected before init() called
    private TransactionPool transactionPool;

    // transaction pool will be injected before init() called
    private ConsortiumRepository consortiumRepository;

    // sub class should set hash policy explicitly when init() called
    private HashPolicy hashPolicy;

    // sub class should set confirmedBlocksProvider explicitly when init() called
    private ConfirmedBlocksProvider confirmedBlocksProvider;

    // sub class should set peer server listener explicitly when init() called
    private PeerServerListener peerServerListener;

    public abstract void init(Properties properties) throws ConsensusEngineInitException;

    public static final ConsensusEngine NONE = new ConsensusEngine() {
        @Override
        public void init(Properties properties) throws ConsensusEngineInitException {
            if(getTransactionPool() == null)
                throw new ConsensusEngineInitException("transaction pool not injected");
            if(getConsortiumRepository() == null)
                throw new ConsensusEngineInitException("consortium repository not injected");
            setMiner(Miner.NONE);
            setValidator(Validator.NONE);
            setStateRepository(StateRepository.NONE);
            setGenesisBlock(new Block());
            setHashPolicy(HashPolicy.NONE);
            setConfirmedBlocksProvider(unconfirmed -> unconfirmed);
            setPeerServerListener(PeerServerListener.NONE);
        }
    };
}
