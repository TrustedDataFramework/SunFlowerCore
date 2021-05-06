package org.tdf.sunflower.consensus.pos;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.tdf.common.event.EventBus;
import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.consensus.AbstractMiner;
import org.tdf.sunflower.consensus.Proposer;
import org.tdf.sunflower.events.NewBlockMined;
import org.tdf.sunflower.facade.BlockRepository;
import org.tdf.sunflower.facade.TransactionPool;
import org.tdf.sunflower.state.Account;
import org.tdf.sunflower.state.StateTrie;
import org.tdf.sunflower.types.*;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.tdf.sunflower.ApplicationConstants.MAX_SHUTDOWN_WAITING;
import static org.tdf.sunflower.types.Transaction.ADDRESS_LENGTH;


@Slf4j(topic = "miner")
public class PoSMiner extends AbstractMiner {

    private final PoS pos;
    @Getter
    public HexBytes minerAddress;
    private ConsensusConfig config;
    @Setter
    private BlockRepository blockRepository;
    private volatile boolean stopped;
    private ScheduledExecutorService minerExecutor;
    @Setter
    @Getter
    private TransactionPool transactionPool;

    public PoSMiner(StateTrie<HexBytes, Account> accountTrie, EventBus eventBus, ConsensusConfig config, PoS pos) {
        super(accountTrie, eventBus, config);
        this.pos = pos;
    }

    @Override
    protected Header createHeader(Block parent) {
        return Header.builder()
                .build();
    }

    @Override
    protected boolean finalizeBlock(Block parent, Block block) {
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
        }, 0, config.getBlockInterval(), TimeUnit.SECONDS);
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
        if (!config.enableMining() || stopped) {
            return;
        }
        if (config.getMinerCoinBase() == null || config.getMinerCoinBase().size() != ADDRESS_LENGTH) {
            log.warn("pos miner: invalid coinbase address {}", config.getMinerCoinBase());
            return;
        }
        Block best = blockRepository.getBestBlock();
        // 判断是否轮到自己出块
        Optional<Proposer> o = getProposer(
                best,
                OffsetDateTime.now().toEpochSecond()
        ).filter(p -> p.getAddress().equals(minerAddress));

        if (!o.isPresent()) return;
        log.debug("try to mining at height " + (best.getHeight() + 1));
        try {
            BlockCreateResult res = createBlock(blockRepository.getBestBlock(), Collections.emptyMap());
            if (res.getBlock() != null) {
                log.info("mining success block: {}", res.getBlock().getHeader());
            }
            getEventBus().publish(new NewBlockMined(res.getBlock(), res.getInfos()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Optional<Proposer> getProposer(Block parent, long currentEpochSeconds) {
        return Optional.empty();
    }


    protected Transaction createCoinBase(long height) {
        return Transaction.builder().build();
    }
}
