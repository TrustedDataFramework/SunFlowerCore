package org.tdf.sunflower.consensus.pow;

import lombok.extern.slf4j.Slf4j;
import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.consensus.AbstractMiner;
import org.tdf.sunflower.events.NewBlockMined;
import org.tdf.sunflower.facade.TransactionPool;
import org.tdf.sunflower.types.Block;
import org.tdf.sunflower.types.Header;
import org.tdf.sunflower.types.Transaction;

import java.util.Optional;
import java.util.Random;
import java.util.concurrent.*;

import static org.tdf.sunflower.ApplicationConstants.MAX_SHUTDOWN_WAITING;

@Slf4j(topic = "pow-miner")
public class PoWMiner extends AbstractMiner {
    private final TransactionPool transactionPool;
    private final PoWConfig poWConfig;
    private final PoW poW;
    private ScheduledExecutorService minerExecutor;
    private final ForkJoinPool threadPool = (ForkJoinPool) Executors.newWorkStealingPool();
    private volatile boolean stopped;

    public PoWMiner(
            PoWConfig minerConfig,
            TransactionPool tp,
            PoW pow
    ) {
        super(pow.getAccountTrie(), pow.getEventBus(), minerConfig);
        this.transactionPool = tp;
        this.poWConfig = minerConfig;
        this.poW = pow;
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
                .payload(HexBytes.fromBytes(poW.getNBits(parent.getStateRoot().getBytes())))
                .build();
    }

    @Override
    protected void finalizeBlock(Block parent, Block block) {
        byte[] nbits = poW.getNBits(parent.getStateRoot().getBytes());
        Random rd = new Random();
        while (PoW.compare(PoW.getPoWHash(block), nbits) > 0) {
            byte[] nonce = new byte[32];
            rd.nextBytes(nonce);
            parent.setPayload(HexBytes.fromBytes(nonce));
        }
    }

    @Override
    public void start() {
        this.stopped = false;
        minerExecutor = Executors.newSingleThreadScheduledExecutor();
        minerExecutor.scheduleAtFixedRate(() -> {
            try {
                this.tryMine();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 0, poWConfig.getBlockInterval(), TimeUnit.SECONDS);
    }

    @Override
    public void stop() {
        if (stopped) return;
        minerExecutor.shutdown();
        try {
            minerExecutor.awaitTermination(MAX_SHUTDOWN_WAITING, TimeUnit.SECONDS);
            threadPool.awaitTermination(MAX_SHUTDOWN_WAITING, TimeUnit.SECONDS);
            log.info("miner stopped normally");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        stopped = true;
    }

    public void tryMine() {
        if (!poWConfig.isEnableMining() || stopped) {
            return;
        }
        if (threadPool.getRunningThreadCount() != 0)
            return;

        Block best = poW.getSunflowerRepository().getBestBlock();
        log.debug("try to mining at height " + (best.getHeight() + 1));
        threadPool.submit(() -> {
            try {
                Optional<Block> b = createBlock(poW.getSunflowerRepository().getBestBlock());
                if (!b.isPresent()) return;
                log.info("mining success block: {}", b.get().getHeader());
                getEventBus().publish(new NewBlockMined(b.get()));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
