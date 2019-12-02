package org.tdf.common;

import org.tdf.exception.ConsensusEngineLoadException;

import java.util.Properties;

public abstract class ConsensusEngine {
    private Miner miner;
    private Validator validator;
    private StateRepository repository;
    private Block genesisBlock;
    private TransactionPool transactionPool;

    public abstract void load(Properties properties, ConsortiumRepository repository) throws ConsensusEngineLoadException;
    public abstract ConfirmedBlocksProvider getProvider();
    public abstract HashPolicy getPolicy();
    public abstract PeerServerListener getHandler();

    public Block getGenesisBlock() {
        return genesisBlock;
    }

    public Validator getValidator() {
        return validator;
    }

    public Miner getMiner() {
        return miner;
    }

    public StateRepository getRepository() {
        return repository;
    }

    public interface Validator extends BlockValidator, PendingTransactionValidator {
    }

    protected void setMiner(Miner miner) {
        this.miner = miner;
    }

    protected void setValidator(Validator validator) {
        this.validator = validator;
    }

    protected void setRepository(StateRepository repository) {
        this.repository = repository;
    }

    protected void setGenesisBlock(Block genesisBlock) {
        this.genesisBlock = genesisBlock;
    }

    protected TransactionPool getTransactionPool(){
        return transactionPool;
    }

    public void setTransactionPool(TransactionPool transactionPool){
        this.transactionPool = transactionPool;
    }
}
