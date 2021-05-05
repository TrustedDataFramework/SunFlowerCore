package org.tdf.sunflower.consensus;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.tdf.common.event.EventBus;
import org.tdf.common.types.Uint256;
import org.tdf.common.util.ByteUtil;
import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.facade.Miner;
import org.tdf.sunflower.facade.TransactionPool;
import org.tdf.sunflower.state.Account;
import org.tdf.sunflower.state.StateTrie;
import org.tdf.sunflower.types.*;
import org.tdf.sunflower.vm.Backend;
import org.tdf.sunflower.vm.CallData;
import org.tdf.sunflower.vm.VMExecutor;

import java.util.*;

@Slf4j(topic = "miner")
public abstract class AbstractMiner implements Miner {
    @Getter(AccessLevel.PROTECTED)
    private final StateTrie<HexBytes, Account> accountTrie;
    @Getter(AccessLevel.PROTECTED)
    private final EventBus eventBus;
    private final ConsensusConfig config;

    public AbstractMiner(StateTrie<HexBytes, Account> accountTrie, EventBus eventBus, ConsensusConfig config) {
        this.config = config;
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

        HexBytes prev = parent.getBody().get(0).getReceiveHex();

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
        PendingData p =
            getTransactionPool().pop(parent.getHeader());

        if (!config.allowEmptyBlock() && p.getPending().isEmpty() )
            return BlockCreateResult.empty();

        Header header = createHeader(parent);
        Block b = new Block(header);

        // get a trie at parent block's state
        // modifications to the trie will not persisted until flush() called

        Transaction coinbase = createCoinBase(parent.getHeight() + 1);

        Backend tmp = accountTrie.createBackend(parent.getHeader(), p.getTrieRoot(),null, false);
        List<Transaction> transactionList = p.getPending();
        b.setBody(transactionList);
        b.getBody().add(0, coinbase);

        Uint256 totalFee =
            p.getReceipts().stream()
                .map(x -> x.getTransaction().getGasPriceAsU256().safeMul(x.getGasUsedAsU256()))
                .reduce(Uint256.ZERO, Uint256::safeAdd);

        // add fee to miners account
        coinbase.setValue(coinbase.getValueAsUint().safeAdd(totalFee));
        CallData callData = CallData.fromTransaction(coinbase, true);
        tmp.setHeaderCreatedAt(System.currentTimeMillis() / 1000);
        header.setCreatedAt(tmp.getHeaderCreatedAt());

        VMExecutor executor = new VMExecutor(tmp, callData);
        VMResult res = executor.execute();

        long lastGas =
            p.getReceipts().isEmpty() ? 0 :
                p.getReceipts().get(p.getReceipts().size() - 1).getCumulativeGasLong();

        TransactionReceipt receipt = new TransactionReceipt(
                p.getTrieRoot().getBytes()
                ,
                // coinbase consume none gas
                ByteUtil.longToBytesNoLeadZeroes(res.getGasUsed() + lastGas),
                new Bloom(),
                Collections.emptyList()
        );
        receipt.setExecutionResult(res.getExecutionResult());
        receipt.setTransaction(coinbase);
        p.getReceipts().add(0, receipt);

        // calculate state root and receipts root
        b.setStateRoot(
                tmp.merge()
        );

        b.setReceiptTrieRoot(TransactionReceipt.calcReceiptsTrie(p.getReceipts()));
        // persist modifications of trie to database
        b.resetTransactionsRoot();

        // the mined block cannot be modified any more
        if (!finalizeBlock(parent, b)) {
            return BlockCreateResult.empty();
        }

        List<TransactionInfo> infos = new ArrayList<>();
        for (int i = 0; i < p.getReceipts().size(); i++) {
            infos.add(new TransactionInfo(p.getReceipts().get(i), b.getHash().getBytes(), i));
        }

        return new BlockCreateResult(b, infos);
    }
}
