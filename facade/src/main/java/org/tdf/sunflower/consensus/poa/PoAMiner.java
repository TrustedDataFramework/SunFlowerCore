package org.tdf.sunflower.consensus.poa;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.tdf.common.crypto.ECDSASignature;
import org.tdf.common.crypto.ECKey;
import org.tdf.common.event.EventBus;
import org.tdf.common.util.BigIntegers;
import org.tdf.common.util.HexBytes;
import org.tdf.common.util.RLPUtil;
import org.tdf.sunflower.consensus.AbstractMiner;
import org.tdf.sunflower.consensus.Proposer;
import org.tdf.sunflower.consensus.poa.config.PoAConfig;
import org.tdf.sunflower.events.NewBlockMined;
import org.tdf.sunflower.facade.BlockRepository;
import org.tdf.sunflower.facade.TransactionPool;
import org.tdf.sunflower.state.Account;
import org.tdf.sunflower.state.StateTrie;
import org.tdf.sunflower.types.Block;
import org.tdf.sunflower.types.BlockCreateResult;
import org.tdf.sunflower.types.Header;
import org.tdf.sunflower.types.Transaction;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.tdf.common.util.ByteUtil.EMPTY_BYTE_ARRAY;
import static org.tdf.common.util.ByteUtil.longToBytesNoLeadZeroes;


@Slf4j(topic = "miner")
public class PoAMiner extends AbstractMiner {
    public static final int MAX_SHUTDOWN_WAITING = 5;
    private final PoA poA;

    HexBytes privateKey;

    HexBytes publicKey;
    private PoAConfig config;
    @Setter
    private BlockRepository blockRepository;
    private volatile boolean stopped;
    private ScheduledExecutorService minerExecutor;
    @Setter
    @Getter
    private TransactionPool transactionPool;

    public PoAMiner(StateTrie<HexBytes, Account> accountTrie, EventBus eventBus, PoAConfig config, PoA poA) {
        super(accountTrie, eventBus, config);
        this.poA = poA;
    }


    @Override
    protected Header createHeader(Block parent) {
        return Header
            .builder()
            .height(parent.getHeight() + 1)
            .createdAt(System.currentTimeMillis() / 1000)
            .coinbase(config.getMinerCoinBase())
            .hashPrev(parent.getHash())
            .gasLimit(parent.getGasLimit())
            .build();
    }

    @Override
    protected boolean finalizeBlock(Block parent, Block block) {
        byte[] rawHash = PoaUtils.getRawHash(block.getHeader());
        ECKey key = ECKey.fromPrivate(privateKey.getBytes());
        ECDSASignature sig = key.sign(rawHash);

        block.setExtraData(
            RLPUtil.encode(
                new Object[]{
                    Byte.toUnsignedInt(sig.v),
                    BigIntegers.asUnsignedByteArray(sig.r),
                    BigIntegers.asUnsignedByteArray(sig.s)}
            )
        );


        if (config.getThreadId() == PoA.GATEWAY_ID) {
            for (int i = 1; i < block.getBody().size(); i++) {
                poA.farmBaseTransactions.add(block.getBody().get(i));
            }
        }
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

        Block best = blockRepository.getBestBlock();
        // 判断是否轮到自己出块
        Optional<Proposer> o = poA.getProposer(
            best,
            OffsetDateTime.now().toEpochSecond()
        ).filter(p -> p.getAddress().equals(config.getMinerCoinBase()));
        if (!o.isPresent()) return;
        log.debug("try to mining at height " + (best.getHeight() + 1));
        try {
            BlockCreateResult b = createBlock(blockRepository.getBestBlock());
            if (b.getBlock() != null) {
                log.info("mining success block: {}", b.getBlock().getHeader());
            }
            getEventBus().publish(new NewBlockMined(b.getBlock(), b.getInfos()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected Transaction createCoinBase(long height) {
        return Transaction.builder()
            .nonce(longToBytesNoLeadZeroes(height))
            .data(publicKey.getBytes())
            .value(poA.economicModel.getConsensusRewardAtHeight(height).getNoLeadZeroesData())
            .data(publicKey.getBytes())
            .receiveAddress(config.getMinerCoinBase().getBytes())
            .gasPrice(EMPTY_BYTE_ARRAY)
            .gasLimit(EMPTY_BYTE_ARRAY)
            .build();
    }

}
