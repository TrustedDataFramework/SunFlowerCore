package org.tdf.sunflower.consensus;

import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.tdf.common.event.EventBus;
import org.tdf.common.types.Uint256;
import org.tdf.common.util.ByteUtil;
import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.facade.Miner;
import org.tdf.sunflower.facade.RepositoryReader;
import org.tdf.sunflower.facade.TransactionPool;
import org.tdf.sunflower.state.Account;
import org.tdf.sunflower.state.StateTrie;
import org.tdf.sunflower.types.*;
import org.tdf.sunflower.vm.Backend;
import org.tdf.sunflower.vm.CallContext;
import org.tdf.sunflower.vm.CallData;
import org.tdf.sunflower.vm.VMExecutor;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public abstract class AbstractMiner implements Miner {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger("miner");
    private final StateTrie<HexBytes, Account> accountTrie;
    private final EventBus eventBus;
    private final ConsensusConfig config;
    protected final TransactionPool pool;

    public AbstractMiner(StateTrie<HexBytes, Account> accountTrie, EventBus eventBus, ConsensusConfig config, TransactionPool pool) {
        this.accountTrie = accountTrie;
        this.eventBus = eventBus;
        this.config = config;
        this.pool = pool;
    }

    protected abstract Transaction createCoinBase(long height);

    protected abstract Header createHeader(Block parent);

    protected abstract boolean finalizeBlock(Block parent, Block block);

    // TODO:  2. 增加打包超时时间
    @SneakyThrows
    protected BlockCreateResult createBlock(RepositoryReader rd, Block parent, Map<String, ?> headerArgs) {
        PendingData p =
            pool.pop(parent.getHeader());

        if (!config.allowEmptyBlock() && p.getPending().isEmpty()) {
            pool.reset(parent.getHeader());
            return BlockCreateResult.empty();
        }

        Header header = createHeader(parent);

        for (Map.Entry<String, ?> entry : headerArgs.entrySet()) {
            String field = entry.getKey();
            Method setter =
                header.getClass()
                    .getMethod(
                        "set" + field.substring(0, 1).toUpperCase() + field.substring(1),
                        entry.getValue().getClass()
                    );
            setter.invoke(header, entry.getValue());
        }

        Block b = new Block(header);

        // get a trie at parent block's state
        // modifications to the trie will not persisted until flush() called

        Transaction coinbase = createCoinBase(parent.getHeight() + 1);
        Backend tmp = p.getCurrent() == null ?
            accountTrie.createBackend(parent.getHeader(), parent.getStateRoot(), null, false)
            : p.getCurrent();
        List<Transaction> transactionList = p.getPending();
        b.setBody(transactionList);
        b.getBody().add(0, coinbase);

        Uint256 totalFee =
            p.getReceipts().stream()
                .map(x -> x.getTransaction().getGasPriceAsU256().times(x.getGasUsedAsU256()))
                .reduce(Uint256.ZERO, Uint256::plus);

        // add fee to miners account
        coinbase.setValue(coinbase.getValueAsUint().plus(totalFee));

        CallContext ctx = CallContext.fromTx(coinbase);
        CallData callData = CallData.fromTx(coinbase, true);
        tmp.setHeaderCreatedAt(System.currentTimeMillis() / 1000);
        header.setCreatedAt(tmp.getHeaderCreatedAt());

        VMResult res;

        VMExecutor executor = new VMExecutor(rd, tmp, ctx, callData, 0);
        res = executor.execute();


        long lastGas =
            p.getReceipts().isEmpty() ? 0 :
                p.getReceipts().get(p.getReceipts().size() - 1).getCumulativeGasLong();

        TransactionReceipt receipt = new TransactionReceipt(
            // coinbase consume none gas
            ByteUtil.longToBytesNoLeadZeroes(res.getGasUsed() + lastGas),
            new Bloom(),
            Collections.emptyList()
        );
        receipt.setExecutionResult(res.getExecutionResult().getBytes());
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

    protected StateTrie<HexBytes, Account> getAccountTrie() {
        return this.accountTrie;
    }

    protected EventBus getEventBus() {
        return this.eventBus;
    }
}
