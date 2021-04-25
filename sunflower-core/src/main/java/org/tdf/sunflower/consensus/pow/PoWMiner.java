package org.tdf.sunflower.consensus.pow;

import lombok.extern.slf4j.Slf4j;
import org.tdf.common.types.Uint256;
import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.consensus.AbstractMiner;
import org.tdf.sunflower.events.NewBestBlock;
import org.tdf.sunflower.events.NewBlockMined;
import org.tdf.sunflower.facade.TransactionPool;
import org.tdf.sunflower.types.Block;
import org.tdf.sunflower.types.BlockCreateResult;
import org.tdf.sunflower.types.Header;
import org.tdf.sunflower.types.Transaction;

import java.util.Random;
import java.util.concurrent.*;

import static org.tdf.sunflower.ApplicationConstants.MAX_SHUTDOWN_WAITING;
import static org.tdf.sunflower.state.Account.ADDRESS_SIZE;

@Slf4j(topic = "pow-miner")
public class PoWMiner extends AbstractMiner {
    private final TransactionPool transactionPool;
    private final PoWConfig poWConfig;
    private final PoW poW;
    private final ForkJoinPool threadPool = (ForkJoinPool) Executors.newWorkStealingPool();
    private ScheduledExecutorService minerExecutor;
    private volatile boolean stopped;

    private volatile long currentMiningHeight;
    private volatile boolean working;
    private Future<?> task;

    public PoWMiner(
            PoWConfig minerConfig,
            TransactionPool tp,
            PoW pow
    ) {
        super(pow.getAccountTrie(), pow.getEventBus(), minerConfig);
        this.transactionPool = tp;
        this.poWConfig = minerConfig;
        this.poW = pow;

        getEventBus().subscribe(NewBestBlock.class, e -> {
            if (e.getBlock().getHeight() >= this.currentMiningHeight) {
                // cancel mining
                this.working = false;
                this.currentMiningHeight = 0;
            }
        });
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
                HexBytes.EMPTY, 0, Uint256.ZERO,
                Uint256.of(20), HexBytes.EMPTY, poWConfig.getMinerCoinBase(),
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
    protected boolean finalizeBlock(Block parent, Block block) {
        byte[] nbits = poW.getNBits(parent.getStateRoot().getBytes());
        Random rd = new Random();
        log.info("start finish pow target = {}", HexBytes.fromBytes(nbits));
        this.working = true;
        while (PoW.compare(PoW.getPoWHash(block), nbits) > 0) {
            if (!working) {
                log.info("mining canceled");
                return false;
            }
            byte[] nonce = new byte[32];
            rd.nextBytes(nonce);
            block.setPayload(HexBytes.fromBytes(nonce));
        }
        log.info("pow success");
        this.working = false;
        return true;
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
        }, 0, Math.max(1, poWConfig.getBlockInterval() / 4), TimeUnit.SECONDS);
    }

    @Override
    public void stop() {
        if (stopped) return;
        this.working = false;
        minerExecutor.shutdown();
        try {
            minerExecutor.awaitTermination(MAX_SHUTDOWN_WAITING, TimeUnit.SECONDS);
            threadPool.awaitTermination(MAX_SHUTDOWN_WAITING, TimeUnit.SECONDS);
            log.info("miner stopped normally");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        this.task = null;
        stopped = true;
    }

    @Override
    public HexBytes getMinerAddress() {
        return poWConfig.getMinerCoinBase();
    }

    public void tryMine() {
        if (!poWConfig.isEnableMining() || stopped) {
            return;
        }
        if (poWConfig.getMinerCoinBase() == null || poWConfig.getMinerCoinBase().size() != ADDRESS_SIZE) {
            log.warn("pow miner: invalid coinbase address {}", poWConfig.getMinerCoinBase());
            return;
        }
        if (working || task != null) return;

        Block best = poW.getSunflowerRepository().getBestBlock();
        log.debug("try to mining at height " + (best.getHeight() + 1));
        this.currentMiningHeight = best.getHeight() + 1;
        long current = System.currentTimeMillis() / 1000;
        if (current <= best.getCreatedAt())
            return;
        Runnable task = () -> {
            try {
                BlockCreateResult res = createBlock(poW.getSunflowerRepository().getBestBlock());
                if (res.getBlock() != null) {
                    log.info("mining success block: {}", res.getBlock().getHeader());
                }
                getEventBus().publish(new NewBlockMined(res.getBlock(), res.getFailedTransactions(), res.getReasons()));
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                this.task = null;
            }
        };
        this.task = threadPool.submit(task);
    }
}
