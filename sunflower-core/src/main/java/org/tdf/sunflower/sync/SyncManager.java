package org.tdf.sunflower.sync;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.tdf.common.event.EventBus;
import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.ApplicationConstants;
import org.tdf.sunflower.SyncConfig;
import org.tdf.sunflower.events.NewBlockMined;
import org.tdf.sunflower.facade.ConsensusEngineFacade;
import org.tdf.sunflower.facade.PeerServerListener;
import org.tdf.sunflower.facade.SunflowerRepository;
import org.tdf.sunflower.facade.TransactionPool;
import org.tdf.sunflower.net.Context;
import org.tdf.sunflower.net.Peer;
import org.tdf.sunflower.net.PeerServer;
import org.tdf.sunflower.types.Block;
import org.tdf.sunflower.types.Transaction;
import org.tdf.sunflower.types.ValidateResult;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * sync manager for full-nodes
 */
@Component
@Slf4j
public class SyncManager implements PeerServerListener {
    private final PeerServer peerServer;
    private final ConsensusEngineFacade engine;
    private final SunflowerRepository sunflowerRepository;
    private final TransactionPool transactionPool;
    private final TreeSet<Block> queue = new TreeSet<>(Block.FAT_COMPARATOR);
    private final SyncConfig syncConfig;
    private final EventBus eventBus;
    private Cache<HexBytes, Boolean> receivedTransactions = CacheBuilder.newBuilder()
            .maximumSize(ApplicationConstants.P2P_TRANSACTION_CACHE_SIZE)
            .build();
    private Cache<HexBytes, Boolean> receivedProposals = CacheBuilder.newBuilder()
            .maximumSize(ApplicationConstants.P2P_PROPOSAL_CACHE_SIZE)
            .build();
    private Lock lock = new ReentrantLock();
    private ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    public SyncManager(
            PeerServer peerServer, ConsensusEngineFacade engine,
            SunflowerRepository sunflowerRepository,
            TransactionPool transactionPool, SyncConfig syncConfig,
            EventBus eventBus
    ) {
        this.peerServer = peerServer;
        this.engine = engine;
        this.sunflowerRepository = sunflowerRepository;
        this.transactionPool = transactionPool;
        this.syncConfig = syncConfig;
        this.eventBus = eventBus;
    }

    @PostConstruct
    public void init() {
        peerServer.addListeners(this);
        executorService.scheduleWithFixedDelay(
                this::tryWrite, 0,
                syncConfig.getBlockWriteRate(), TimeUnit.SECONDS);

        executorService.scheduleWithFixedDelay(
                this::sendStatus, 0,
                syncConfig.getHeartRate(), TimeUnit.SECONDS
        );
        eventBus.subscribe(NewBlockMined.class, (e) -> {
            propose(e.getBlock());
        });
    }

    @Override
    public void onMessage(Context context, PeerServer server) {
        Optional<SyncMessage> o = SyncMessage.decode(context.getMessage());
        if (!o.isPresent()) return;
        SyncMessage msg = o.get();
        switch (msg.getCode()) {
            case SyncMessage.UNKNOWN:
                return;
            case SyncMessage.STATUS: {
                Status s = msg.getBodyAs(Status.class);
                this.onStatus(context, s);
                return;
            }
            case SyncMessage.GET_BLOCKS: {
                GetBlocks getBlocks = msg.getBodyAs(GetBlocks.class);
                List<Block> blocks;
                if (getBlocks.isDescend()) {
                    blocks = sunflowerRepository.getBlocksBetweenDescend(
                            getBlocks.getStartHeight(),
                            getBlocks.getStopHeight(),
                            syncConfig.getMaxBlocksTransfer()
                    );
                } else {
                    blocks = sunflowerRepository.getBlocksBetween(
                            getBlocks.getStartHeight(),
                            getBlocks.getStopHeight(),
                            syncConfig.getMaxBlocksTransfer()
                    );
                }
                context.response(SyncMessage.encode(SyncMessage.BLOCKS, blocks));
                return;
            }
            case SyncMessage.TRANSACTIONS: {
                Transaction tx = msg.getBodyAs(Transaction.class);
                if (receivedTransactions.asMap().containsKey(tx.getHash())) return;
                receivedTransactions.put(tx.getHash(), true);
                context.relay();
                transactionPool.collect(tx);
                return;
            }
            case SyncMessage.PROPOSAL: {
                Block best = sunflowerRepository.getBestBlock();
                Block proposal = msg.getBodyAs(Block.class);
                if (receivedProposals.asMap().containsKey(proposal.getHash())) {
                    return;
                }
                receivedProposals.put(proposal.getHash(), true);
                context.relay();
                if (Math.abs(proposal.getHeight() - best.getHeight()) > syncConfig.getMaxPendingBlocks()) {
                    return;
                }
                if (sunflowerRepository.containsBlock(proposal.getHash().getBytes()))
                    return;
                lock.lock();
                try {
                    queue.add(proposal);
                } finally {
                    lock.unlock();
                }
                return;
            }
            case SyncMessage.BLOCKS: {
                Block best = sunflowerRepository.getBestBlock();
                Block[] blocks = msg.getBodyAs(Block[].class);
                Arrays.sort(blocks, Block.FAT_COMPARATOR);
                lock.lock();
                try {
                    for (Block block : blocks) {
                        if (Math.abs(block.getHeight() - best.getHeight()) > syncConfig.getMaxPendingBlocks())
                            break;
                        if (sunflowerRepository.containsBlock(block.getHash().getBytes()))
                            continue;
                        queue.add(block);
                    }
                } finally {
                    lock.unlock();
                }
                return;
            }
        }
    }

    private void onStatus(Context ctx, Status s) {
        Block b = null;
        Block best = sunflowerRepository.getBestBlock();
        lock.lock();
        try {
            b = queue.first();
        } catch (NoSuchElementException ignored) {

        } finally {
            lock.unlock();
        }
        if (b != null && s.getBestBlockHeight() > b.getHeight()) {
            GetBlocks getBlocks = new GetBlocks(best.getHeight(), b.getHeight(), true);
            ctx.response(SyncMessage.encode(SyncMessage.GET_BLOCKS, getBlocks));
        }
        if (s.getBestBlockHeight() >= best.getHeight() && !s.getBestBlockHash().equals(best.getHash())) {
            GetBlocks getBlocks = new GetBlocks(best.getHeight(), s.getBestBlockHeight(), false);
            ctx.response(SyncMessage.encode(SyncMessage.GET_BLOCKS, getBlocks));
        }
    }

    public void tryWrite() {
        lock.lock();
        try {
            while (true) {
                Block b = null;
                try {
                    b = queue.first();
                } catch (NoSuchElementException ignored) {
                }
                if (b == null) return;
                if (sunflowerRepository.containsBlock(b.getHash().getBytes())) {
                    queue.remove(b);
                    continue;
                }
                Optional<Block> o = sunflowerRepository.getBlock(b.getHashPrev().getBytes());
                if (!o.isPresent()) {
                    return;
                }
                ValidateResult res = engine.getValidator().validate(b, o.get());
                if (!res.isSuccess()) {
                    queue.remove(b);
                    log.error(res.getReason());
                    continue;
                }
                queue.remove(b);
                sunflowerRepository.writeBlock(b);
            }
        } finally {
            lock.unlock();
        }
    }

    public void sendStatus() {
        Block best = sunflowerRepository.getBestBlock();
        Block genesis = sunflowerRepository.getGenesis();
        Status status = new Status(best.getHeight(), best.getHash(), genesis.getHash());
        peerServer.broadcast(SyncMessage.encode(SyncMessage.STATUS, status));
    }


    public void propose(Block b) {
        peerServer.broadcast(SyncMessage.encode(SyncMessage.PROPOSAL, b));
    }

    @Override
    public void onStart(PeerServer server) {

    }

    @Override
    public void onNewPeer(Peer peer, PeerServer server) {

    }

    @Override
    public void onDisconnect(Peer peer, PeerServer server) {

    }
}
