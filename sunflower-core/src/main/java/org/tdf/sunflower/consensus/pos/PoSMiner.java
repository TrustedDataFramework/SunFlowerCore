package org.tdf.sunflower.consensus.pos;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.tdf.common.event.EventBus;
import org.tdf.common.types.Uint256;
import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.consensus.AbstractMiner;
import org.tdf.sunflower.consensus.MinerConfig;
import org.tdf.sunflower.consensus.Proposer;
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
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.tdf.sunflower.ApplicationConstants.MAX_SHUTDOWN_WAITING;
import static org.tdf.sunflower.state.Account.ADDRESS_SIZE;


@Slf4j(topic = "miner")
public class PoSMiner extends AbstractMiner {

    private PoSConfig posConfig;

    @Getter
    public HexBytes minerAddress;

    @Setter
    private BlockRepository blockRepository;

    private volatile boolean stopped;

    private ScheduledExecutorService minerExecutor;

    @Setter
    @Getter
    private TransactionPool transactionPool;

    private final PoS pos;

    public PoSMiner(StateTrie<HexBytes, Account> accountTrie, EventBus eventBus, MinerConfig minerConfig, PoS pos) {
        super(accountTrie, eventBus, minerConfig);
        this.pos = pos;
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

    public void setPoSConfig(PoSConfig posConfig) throws ConsensusEngineInitException {
        this.posConfig = posConfig;
        this.minerAddress = posConfig.getMinerCoinBase();
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
        }, 0, posConfig.getBlockInterval(), TimeUnit.SECONDS);
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
        if (!posConfig.isEnableMining() || stopped) {
            return;
        }
        if(posConfig.getMinerCoinBase() == null || posConfig.getMinerCoinBase().size() != ADDRESS_SIZE){
            log.warn("pos miner: invalid coinbase address {}", posConfig.getMinerCoinBase());
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
            Optional<Block> b = createBlock(blockRepository.getBestBlock());
            if (!b.isPresent()) return;
            log.info("mining success block: {}", b.get().getHeader());
            getEventBus().publish(new NewBlockMined(b.get()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Optional<Proposer> getProposer(Block parent, long currentEpochSeconds) {
        List<HexBytes> minerAddresses = pos.getMinerAddresses(parent.getStateRoot().getBytes());
        return AbstractMiner.getProposer(parent, currentEpochSeconds, minerAddresses, posConfig.getBlockInterval());
    }


    protected Transaction createCoinBase(long height) {
        return Transaction.builder()
                .version(PoS.TRANSACTION_VERSION)
                .createdAt(System.currentTimeMillis() / 1000)
                .nonce(height)
                .from(HexBytes.EMPTY)
                .amount(Uint256.ZERO)
                .payload(HexBytes.empty())
                .to(this.minerAddress)
                .gasPrice(Uint256.ZERO)
                .signature(HexBytes.EMPTY).build();
    }
}
