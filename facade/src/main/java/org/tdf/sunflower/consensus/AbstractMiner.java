package org.tdf.sunflower.consensus;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.tdf.common.event.EventBus;
import org.tdf.common.store.CachedStore;
import org.tdf.common.store.Store;
import org.tdf.sunflower.types.TransactionResult;
import org.tdf.common.util.ByteArrayMap;
import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.events.TransactionFailed;
import org.tdf.sunflower.events.TransactionIncluded;
import org.tdf.sunflower.facade.Miner;
import org.tdf.sunflower.facade.TransactionPool;
import org.tdf.sunflower.state.Account;
import org.tdf.sunflower.state.Constants;
import org.tdf.sunflower.state.ForkedStateTrie;
import org.tdf.sunflower.state.StateTrie;
import org.tdf.sunflower.types.Block;
import org.tdf.sunflower.types.Header;
import org.tdf.sunflower.types.Transaction;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j(topic = "miner")
public abstract class AbstractMiner implements Miner {
    public static Optional<Proposer> getProposer(Block parent, long currentEpochSeconds, List<HexBytes> minerAddresses, long blockInterval) {
        if (currentEpochSeconds - parent.getCreatedAt() < blockInterval) {
            return Optional.empty();
        }
        if (parent.getHeight() == 0) {
            return Optional.of(new Proposer(minerAddresses.get(0), 0, Long.MAX_VALUE));
        }

        HexBytes prev = parent.getBody().get(0).getTo();

        int prevIndex = minerAddresses.indexOf(prev);

        if (prevIndex < 0)
            prevIndex += minerAddresses.size();

        long step = (currentEpochSeconds - parent.getCreatedAt())
                / blockInterval;

        int currentIndex = (int) ((prevIndex + step) % minerAddresses.size());
        long startTime = parent.getCreatedAt() + step * blockInterval;
        long endTime = startTime + blockInterval;

        return Optional.of(new Proposer(
                minerAddresses.get(currentIndex),
                startTime,
                endTime
        ));
    }


    @Getter(AccessLevel.PROTECTED)
    private final StateTrie<HexBytes, Account> accountTrie;

    @Getter(AccessLevel.PROTECTED)
    private final EventBus eventBus;

    private final MinerConfig minerConfig;

    public AbstractMiner(StateTrie<HexBytes, Account> accountTrie, EventBus eventBus, MinerConfig minerConfig) {
        this.minerConfig = minerConfig;
        this.accountTrie = accountTrie;
        this.eventBus = eventBus;
    }

    protected abstract TransactionPool getTransactionPool();

    protected abstract Transaction createCoinBase(long height);

    protected abstract Header createHeader(Block parent);

    protected abstract void finalizeBlock(Block parent, Block block);

    protected Optional<Block> createBlock(Block parent) {
        if (!minerConfig.isAllowEmptyBlock() && getTransactionPool().size() == 0)
            return Optional.empty();

        Header header = createHeader(parent);
        Block b = new Block(header);
        Store<byte[], byte[]> cache = new CachedStore<>(accountTrie.getTrieStore(), ByteArrayMap::new);

        // get a trie at parent block's state
        // modifications to the trie will not persisted until flush() called
        ForkedStateTrie<HexBytes, Account> tmp = accountTrie.fork(parent.getStateRoot().getBytes());


        Transaction coinbase = createCoinBase(parent.getHeight() + 1);
        List<Transaction> transactionList = getTransactionPool().popPackable(
                getAccountTrie().getTrie(parent.getStateRoot().getBytes()),
                minerConfig.getMaxBodySize()
        );

        transactionList.add(0, coinbase);
        Map<HexBytes, TransactionResult> m = new HashMap<>();
        for (Transaction tx : transactionList) {
            // try to fetch transaction from pool
            try {

                // get all account related to this transaction in the trie

                // store updated result to the trie if update success
                TransactionResult res = tmp.update(header, tx);
                m.put(tx.getHash(), res);
            } catch (Exception e) {
                // prompt reason for failed updates
                e.printStackTrace();
                log.error("execute transaction " + tx.getHash() + " failed, reason = " + e.getMessage());
                getTransactionPool().drop(tx);
                eventBus.publish(new TransactionFailed(tx, e.getMessage()));
                continue;
            }
            b.getBody().add(tx);
        }

        // transactions may failed to execute
        if (b.getBody().size() == 1 && !minerConfig.isAllowEmptyBlock())
            return Optional.empty();

        // add fee to miners account
        Account feeAccount = tmp.get(Constants.FEE_ACCOUNT_ADDR).get();
        tmp.remove(Constants.FEE_ACCOUNT_ADDR);

        coinbase.setAmount(coinbase.getAmount().safeAdd(feeAccount.getBalance()));
        tmp.update(header, coinbase);

        // calculate state root
        b.setStateRoot(
                HexBytes.fromBytes(tmp.commit())
        );

        // persist modifications of trie to database
        tmp.flush();
        b.resetTransactionsRoot();

        // the mined block cannot be modified any more
        finalizeBlock(parent, b);

        b.getBody().stream().skip(1)
                .forEach(tx -> {
                    TransactionResult res = m.get(tx.getHash());
                    eventBus.publish(new TransactionIncluded(tx, b, res.getGasUsed(), res.getReturns(), res.getEvents())); });
        return Optional.of(b);
    }
}
