package org.tdf.sunflower.consensus.poa;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.tdf.common.event.EventBus;
import org.tdf.common.util.HexBytes;
import org.tdf.crypto.keystore.KeyStoreImpl;
import org.tdf.sunflower.facade.KeyStore;
import org.tdf.sunflower.state.Address;
import org.tdf.sunflower.consensus.AbstractMiner;
import org.tdf.sunflower.consensus.MinerConfig;
import org.tdf.sunflower.consensus.Proposer;
import org.tdf.sunflower.crypto.CryptoHelpers;
import org.tdf.sunflower.events.NewBlockMined;
import org.tdf.sunflower.exception.ConsensusEngineInitException;
import org.tdf.sunflower.facade.BlockRepository;
import org.tdf.sunflower.facade.TransactionPool;
import org.tdf.sunflower.state.Account;
import org.tdf.sunflower.state.StateTrie;
import org.tdf.sunflower.types.Block;
import org.tdf.sunflower.types.Header;
import org.tdf.sunflower.types.Transaction;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.tdf.sunflower.ApplicationConstants.MAX_SHUTDOWN_WAITING;


@Slf4j(topic = "miner")
public class PoAMiner extends AbstractMiner {
    private PoAConfig poAConfig;

    HexBytes minerAddress;

    @Setter
    private BlockRepository blockRepository;

    private volatile boolean stopped;

    private ScheduledExecutorService minerExecutor;

    @Setter
    @Getter
    private TransactionPool transactionPool;

    private final PoA poA;

    public PoAMiner(StateTrie<HexBytes, Account> accountTrie, EventBus eventBus, MinerConfig minerConfig, PoA poA) {
        super(accountTrie, eventBus, minerConfig);
        this.poA = poA;
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
    protected void finalizeBlock(Block parent, Block block) {

    }

    public void setPoAConfig(PoAConfig poAConfig) throws ConsensusEngineInitException {
        this.poAConfig = poAConfig;
        this.minerAddress = poAConfig.getMinerCoinBase();
        if (poA.getKeyStore() != KeyStore.NONE)
            this.minerAddress = Address.fromPublicKey(
                    CryptoHelpers.getPkFromSk(poA.getKeyStore().getPrivateKey().getBytes())
            );
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
        Optional<Proposer> o = poA.getProposer(
                best,
                OffsetDateTime.now().toEpochSecond()
        ).filter(p -> p.getAddress().equals(minerAddress));
        if (!o.isPresent()) return;
        log.debug("try to mining at height " + (best.getHeight() + 1));
        try {
            Optional<Block> b = createBlock(blockRepository.getBestBlock());
            if (!b.isPresent()) return;
            log.info("mining success block: {}", b.get().getHeader());
            getEventBus().publish(new NewBlockMined(b.get()));
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
                .payload(HexBytes.empty())
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
