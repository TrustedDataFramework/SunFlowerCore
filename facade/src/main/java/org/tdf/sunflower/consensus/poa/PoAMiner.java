package org.tdf.sunflower.consensus.poa;

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
import org.tdf.sunflower.facade.BlockRepository;
import org.tdf.sunflower.facade.SecretStore;
import org.tdf.sunflower.facade.TransactionPool;
import org.tdf.sunflower.state.Account;
import org.tdf.sunflower.state.Address;
import org.tdf.sunflower.state.StateTrie;
import org.tdf.sunflower.types.*;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.tdf.common.util.ByteUtil.*;


@Slf4j(topic = "miner")
public class PoAMiner extends AbstractMiner {
    public static final int MAX_SHUTDOWN_WAITING = 5;
    private final PoA poA;
    @Getter
    public HexBytes minerAddress;

    HexBytes privateKey;

    HexBytes publicKey;
    private PoAConfig poAConfig;
    @Setter
    private BlockRepository blockRepository;
    private volatile boolean stopped;
    private ScheduledExecutorService minerExecutor;
    @Setter
    @Getter
    private TransactionPool transactionPool;

    public PoAMiner(StateTrie<HexBytes, Account> accountTrie, EventBus eventBus, MinerConfig minerConfig, PoA poA) {
        super(accountTrie, eventBus, minerConfig);
        this.poA = poA;
    }


    @Override
    protected Header createHeader(Block parent) {
        return Header.builder()
                .build();
    }

    @Override
    protected boolean finalizeBlock(Block parent, Block block) {
        byte[] plain = PoA.getSignaturePlain(block);
        byte[] sig = CryptoContext.sign(privateKey.getBytes(), plain);
//        block.setPayload(HexBytes.fromBytes(sig));

        if (poAConfig.getRole().equals("gateway")) {
            for (int i = 1; i < block.getBody().size(); i++) {
                poA.farmBaseTransactions.add(block.getBody().get(i));
            }
        }
        return true;
    }

    public void setPoAConfig(PoAConfig poAConfig) {
        this.poAConfig = poAConfig;
        this.privateKey = (poA.getSecretStore() != SecretStore.NONE) ?
                poA.getSecretStore().getPrivateKey() :
                HexBytes.fromHex(poAConfig.getPrivateKey());

        this.publicKey = HexBytes.fromBytes(CryptoContext.getPkFromSk(privateKey.getBytes()));
        this.minerAddress = Address.fromPublicKey(
                CryptoContext.getPkFromSk(privateKey.getBytes())
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
            BlockCreateResult b = createBlock(blockRepository.getBestBlock());
            if (b.getBlock() != null) {
                log.info("mining success block: {}", b.getBlock().getHeader());
            }
            getEventBus().publish(new NewBlockMined(b.getBlock(), b.getFailedTransactions(), b.getReasons(), b.getInfos()));
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
                .receiveAddress(minerAddress.getBytes())
                .gasPrice(EMPTY_BYTE_ARRAY)
                .gasLimit(EMPTY_BYTE_ARRAY)
                .build();
    }

}
