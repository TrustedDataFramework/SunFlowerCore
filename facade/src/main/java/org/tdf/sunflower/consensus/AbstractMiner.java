package org.tdf.sunflower.consensus;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.tdf.common.event.EventBus;
import org.tdf.common.types.Uint256;
import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.events.TransactionFailed;
import org.tdf.sunflower.events.TransactionIncluded;
import org.tdf.sunflower.facade.Miner;
import org.tdf.sunflower.facade.TransactionPool;
import org.tdf.sunflower.state.Account;
import org.tdf.sunflower.state.ForkedStateTrie;
import org.tdf.sunflower.state.StateTrie;
import org.tdf.sunflower.types.*;

import java.util.*;

@Slf4j(topic = "miner")
public abstract class AbstractMiner implements Miner {
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

    protected abstract TransactionPool getTransactionPool();

    protected abstract Transaction createCoinBase(long height);

    protected abstract Header createHeader(Block parent);

    protected abstract boolean finalizeBlock(Block parent, Block block);

    // TODO:  2. 增加打包超时时间
    protected BlockCreateResult createBlock(Block parent) {
        if (!minerConfig.isAllowEmptyBlock() && getTransactionPool().size() == 0)
            return BlockCreateResult.empty();

        Header header = createHeader(parent);
        Block b = new Block(header);

        // get a trie at parent block's state
        // modifications to the trie will not persisted until flush() called
        ForkedStateTrie tmp = accountTrie.fork(parent.getStateRoot().getBytes());


        Transaction coinbase = createCoinBase(parent.getHeight() + 1);
        List<Transaction> transactionList = getTransactionPool().popPackable(
                getAccountTrie().getTrie(parent.getStateRoot().getBytes()),
                minerConfig.getMaxBodySize()
        );

        transactionList.add(0, coinbase);
        Map<HexBytes, TransactionResult> m = new HashMap<>();
        Uint256 totalFee = Uint256.ZERO;

        List<HexBytes> failedTransactions = new ArrayList<>();
        List<String> reasons = new ArrayList<>();

        for (Transaction tx : transactionList.subList(1, transactionList.size())) {
            // try to fetch transaction from pool
            try {

                // get all account related to this transaction in the trie

                // store updated result to the trie if update success
                TransactionResult res = tmp.update(header, tx);
                totalFee = totalFee.safeAdd(res.getFee());
                m.put(tx.getHash(), res);
            } catch (Exception e) {
                // prompt reason for failed updates
                e.printStackTrace();
                failedTransactions.add(tx.getHash());
                reasons.add(e.getMessage());
                log.error("execute transaction " + tx.getHash() + " failed, reason = " + e.getMessage());
                getTransactionPool().drop(tx);
                eventBus.publish(new TransactionFailed(tx.getHash(), e.getMessage()));
                continue;
            }
            b.getBody().add(tx);
        }

        b.getBody().add(0, coinbase);
        // transactions may failed to execute
        if (b.getBody().size() == 1 && !minerConfig.isAllowEmptyBlock())
            return new BlockCreateResult(null, failedTransactions, reasons);

        // add fee to miners account
        coinbase.setAmount(coinbase.getAmount().safeAdd(totalFee));
        tmp.update(header, coinbase);

        // calculate state root
        b.setStateRoot(
                HexBytes.fromBytes(tmp.getCurrentRoot())
        );

        // persist modifications of trie to database
        b.resetTransactionsRoot();

        // the mined block cannot be modified any more
        if(!finalizeBlock(parent, b)){
            return BlockCreateResult.empty();
        }

        b.getBody().stream().skip(1)
                .forEach(tx -> {
                    TransactionResult res = m.get(tx.getHash());
                    eventBus.publish(new TransactionIncluded(tx.getHash(), b, res.getGasUsed(), res.getReturns(), res.getEvents()));
                });
        return new BlockCreateResult(b, failedTransactions, reasons);
    }
}
