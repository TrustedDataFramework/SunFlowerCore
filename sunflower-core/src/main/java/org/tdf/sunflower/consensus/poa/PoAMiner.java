package org.tdf.sunflower.consensus.poa;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.consensus.AbstractMiner;
import org.tdf.sunflower.consensus.poa.config.Genesis;
import org.tdf.sunflower.events.NewBlockMined;
import org.tdf.sunflower.exception.ConsensusEngineInitException;
import org.tdf.sunflower.facade.BlockRepository;
import org.tdf.sunflower.facade.TransactionPool;
import org.tdf.sunflower.types.Block;
import org.tdf.sunflower.types.Header;
import org.tdf.sunflower.types.Transaction;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.tdf.sunflower.ApplicationConstants.MAX_SHUTDOWN_WAITING;


@Slf4j(topic = "poa.miner")
public class PoAMiner extends AbstractMiner {
    private PoAConfig poAConfig;

    HexBytes minerAddress;

    private Genesis genesis;

    @Setter
    private BlockRepository blockRepository;

    private boolean stopped;

    private ScheduledExecutorService minerExecutor;

    private List<HexBytes> minerAddresses;

    @Setter
    @Getter
    private TransactionPool transactionPool;

    @Override
    protected Header createHeader(Block parent) {
        return Header.builder()
                .version(parent.getVersion())
                .hashPrev(parent.getHash()).height(parent.getHeight() + 1)
                .createdAt(System.currentTimeMillis() / 1000)
                .payload(HexBytes.EMPTY)
                .build();
    }

    public void setGenesis(Genesis genesis) {
        this.genesis = genesis;
        this.minerAddresses =
                genesis.miners.stream()
                        .map(Genesis.MinerInfo::getAddress)
                        .collect(Collectors.toList());
    }

    public void setPoAConfig(PoAConfig poAConfig) throws ConsensusEngineInitException {
        this.poAConfig = poAConfig;
        this.minerAddress = poAConfig.getMinerCoinBase();
    }


    public Optional<Proposer> getProposer(Block parent, long currentEpochSeconds) {

        if (currentEpochSeconds - parent.getCreatedAt() < poAConfig.getBlockInterval()) {
            return Optional.empty();
        }
        if (parent.getHeight() == 0) {
            return Optional.of(new Proposer(genesis.miners.get(0).address, 0, Long.MAX_VALUE));
        }

        HexBytes prev = parent.getBody().get(0).getTo();

        int prevIndex = minerAddresses.indexOf(prev);

        if (prevIndex < 0) {
            return Optional.empty();
        }

        long step = (currentEpochSeconds - parent.getCreatedAt())
                / poAConfig.getBlockInterval();

        int currentIndex = (int) ((prevIndex + step) % genesis.miners.size());
        long startTime = parent.getCreatedAt() + step * poAConfig.getBlockInterval();
        long endTime = startTime + poAConfig.getBlockInterval();

        return Optional.of(new Proposer(
                genesis.miners.get(currentIndex).address,
                startTime,
                endTime
        ));
    }


    @Override
    public void start() {
        minerExecutor = Executors.newSingleThreadScheduledExecutor();
        minerExecutor.scheduleAtFixedRate(() -> {
            try {
                this.tryMine();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 0, poAConfig.getBlockInterval(), TimeUnit.SECONDS);
    }

    @Override
    public void stop() {
        if (stopped) return;
        minerExecutor.shutdown();
        try {
            minerExecutor.awaitTermination(MAX_SHUTDOWN_WAITING, TimeUnit.SECONDS);
            log.info("miner stopped normally");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        stopped = true;
    }

    public void tryMine() {
        if (!poAConfig.isEnableMining() || stopped) {
            return;
        }

        Block best = blockRepository.getBestBlock();
        // 判断是否轮到自己出块
        Optional<Proposer> o = getProposer(
                best,
                OffsetDateTime.now().toEpochSecond()
        ).filter(p -> p.getAddress().equals(minerAddress));
        if (!o.isPresent()) return;
        log.info("try to mining at height " + (best.getHeight() + 1));
        try {
            Block b = createBlock(blockRepository.getBestBlock());
            log.info("mining success");
            getEventBus().publish(new NewBlockMined(b));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected Transaction createCoinBase(long height) {
        return Transaction.builder()
                .version(PoAConstants.TRANSACTION_VERSION)
                .createdAt(System.currentTimeMillis() / 1000)
                .nonce(height)
                .from(HexBytes.EMPTY)
                .amount(EconomicModelImpl.getConsensusRewardAtHeight(height))
                .payload(HexBytes.EMPTY)
                .to(minerAddress)
                .signature(HexBytes.EMPTY).build();
    }

    public static class EconomicModelImpl {

        private static final long INITIAL_SUPPLY = 20;

        private static final long HALF_PERIOD = 10000000;


        public static long getConsensusRewardAtHeight(long height) {
            long era = height / HALF_PERIOD;
            long reward = INITIAL_SUPPLY;
            for (long i = 0; i < era; i++) {
                reward = reward * 52218182 / 100000000;
            }
            return reward;
        }

    }
}
