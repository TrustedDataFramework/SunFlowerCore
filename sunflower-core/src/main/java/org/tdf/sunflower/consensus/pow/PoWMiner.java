package org.tdf.sunflower.consensus.pow;

import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.consensus.AbstractMiner;
import org.tdf.sunflower.facade.TransactionPool;
import org.tdf.sunflower.types.Block;
import org.tdf.sunflower.types.Header;
import org.tdf.sunflower.types.Transaction;

public class PoWMiner extends AbstractMiner {
    private final TransactionPool transactionPool;
    private final PoWConfig poWConfig;

    public PoWMiner(PoWConfig minerConfig, TransactionPool tp) {
        super(minerConfig);
        this.transactionPool = tp;
        this.poWConfig = minerConfig;
    }

    @Override
    protected TransactionPool getTransactionPool() {
        return transactionPool;
    }

    @Override
    protected Transaction createCoinBase(long height) {
        return new Transaction(
                PoW.TRANSACTION_VERSION, Transaction.Type.COIN_BASE.code,
                System.currentTimeMillis() / 1000, height,
                HexBytes.EMPTY, 0,
                20, HexBytes.EMPTY, poWConfig.getMinerCoinBase(),
                HexBytes.EMPTY
        );
    }

    @Override
    protected Header createHeader(Block parent) {
        return Header.builder()
                .version(parent.getVersion())
                .hashPrev(parent.getHash()).height(parent.getHeight() + 1)
                .createdAt(System.currentTimeMillis() / 1000)
                .payload(HexBytes.EMPTY)
                .build();
    }

    @Override
    protected void finalizeBlock(Block block) {

    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {

    }
}
