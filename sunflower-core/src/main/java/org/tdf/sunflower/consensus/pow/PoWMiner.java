package org.tdf.sunflower.consensus.pow;

import lombok.extern.slf4j.Slf4j;
import org.tdf.common.types.Uint256;
import org.tdf.common.util.ByteUtil;
import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.consensus.AbstractMiner;
import org.tdf.sunflower.events.NewBestBlock;
import org.tdf.sunflower.events.NewBlockMined;
import org.tdf.sunflower.facade.RepositoryReader;
import org.tdf.sunflower.facade.TransactionPool;
import org.tdf.sunflower.state.Address;
import org.tdf.sunflower.types.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.Random;
import java.util.concurrent.*;

import static org.tdf.sunflower.ApplicationConstants.MAX_SHUTDOWN_WAITING;
import static org.tdf.sunflower.types.Transaction.ADDRESS_LENGTH;

@Slf4j(topic = "pow-miner")
public class PoWMiner extends AbstractMiner {
    private final TransactionPool transactionPool;
    private final ConsensusConfig config;
    private final PoW poW;
    private final ForkJoinPool threadPool = (ForkJoinPool) Executors.newWorkStealingPool();
    private ScheduledExecutorService minerExecutor;
    private volatile boolean stopped;

    private volatile long currentMiningHeight;
    private volatile boolean working;
    private Future<?> task;

    public PoWMiner(
        ConsensusConfig config,
        TransactionPool tp,
        PoW pow
    ) {
        super(pow.getAccountTrie(), pow.getEventBus(), config, pow.getTransactionPool());
        this.transactionPool = tp;
        this.config = config;
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
    protected Transaction createCoinBase(long height) {
        return Transaction.builder()
            .gasLimit(ByteUtil.EMPTY_BYTE_ARRAY)
            .receiveAddress(Address.empty().getBytes())
            .data(PoWBios.UPDATE.encode())
            .value(ByteUtil.EMPTY_BYTE_ARRAY)
            .gasPrice(ByteUtil.EMPTY_BYTE_ARRAY)
            .nonce(ByteUtil.longToBytesNoLeadZeroes(height))
            .build();
    }

    @Override
    protected Header createHeader(Block parent) {
        return Header.builder()
            .hashPrev(parent.getHash())
            .coinbase(config.getMinerCoinBase())
            .createdAt(System.currentTimeMillis() / 1000)
            .height(parent.getHeight() + 1)
            .build();
    }

    @Override
    protected boolean finalizeBlock(Block parent, Block block) {
        Uint256 nbits = poW.bios.getNBits(parent.getHash());
        Random rd = new Random();
        byte[] nonce = new byte[8];
        rd.nextBytes(nonce);
        log.info("start finish pow target = {}", nbits.getDataHex());
        this.working = true;
        while (PoW.compare(PoW.getPoWHash(block), nbits.getData()) > 0) {
            if (!working) {
                log.info("mining canceled");
                return false;
            }
            rd.nextBytes(nonce);
            block.setNonce(HexBytes.fromBytes(Arrays.copyOfRange(nonce, 0, nonce.length)));
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
        }, 0, Math.max(1, config.getBlockInterval() / 4), TimeUnit.SECONDS);
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
        return config.getMinerCoinBase();
    }

    public void tryMine() {
        if (!config.enableMining() || stopped) {
            return;
        }
        if (config.getMinerCoinBase() == null || config.getMinerCoinBase().size() != ADDRESS_LENGTH) {
            log.warn("pow miner: invalid coinbase address {}", config.getMinerCoinBase());
            return;
        }
        if (working || task != null) return;

        try (RepositoryReader rd = poW.getRepo().getReader()) {
            Block best = rd.getBestBlock();
            log.debug("try to mining at height " + (best.getHeight() + 1));
            this.currentMiningHeight = best.getHeight() + 1;
            long current = System.currentTimeMillis() / 1000;
            if (current <= best.getCreatedAt())
                return;
        }

        Runnable task = () -> {
            try (RepositoryReader rd = poW.getRepo().getReader()) {
                BlockCreateResult res = createBlock(
                    rd.getBestBlock(),
                    Collections.emptyMap());
                if (res.getBlock() != null) {
                    log.info("mining success block: {}", res.getBlock().getHeader());
                }
                getEventBus().publish(new NewBlockMined(res.getBlock(), res.getInfos()));
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                this.task = null;
            }
        };
        this.task = threadPool.submit(task);
    }
}
