package org.tdf.sunflower.consensus;

import lombok.extern.slf4j.Slf4j;
import org.tdf.common.store.CachedStore;
import org.tdf.common.store.Store;
import org.tdf.common.trie.Trie;
import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.consensus.vrf.util.ByteArrayMap;
import org.tdf.sunflower.facade.Miner;
import org.tdf.sunflower.facade.TransactionPool;
import org.tdf.sunflower.state.Account;
import org.tdf.sunflower.state.StateTrie;
import org.tdf.sunflower.state.StateUpdater;
import org.tdf.sunflower.types.Block;
import org.tdf.sunflower.types.Header;
import org.tdf.sunflower.types.Transaction;
import org.tdf.sunflower.types.UnmodifiableBlock;

import java.util.*;

@Slf4j
public abstract class AbstractMiner implements Miner {
    protected abstract StateTrie<HexBytes, Account> getAccountTrie();

    protected abstract TransactionPool getTransactionPool();

    protected abstract Transaction createCoinBase(long height);


    protected abstract Header createHeader(Block parent);

    protected Block createBlock(Block parent){
        Header header = createHeader(parent);

        Block b = new Block(header);
        Store<byte[], byte[]> cache = new CachedStore<>(getAccountTrie().getTrieStore(), ByteArrayMap::new);

        // get a trie at parent block's state
        // modifications to the trie will not persisted until flush() called
        Trie<HexBytes, Account> tmp = getAccountTrie()
                .getTrie()
                .revert(parent.getStateRoot().getBytes(), cache);

        StateUpdater<HexBytes, Account> updater = getAccountTrie().getUpdater();
        Transaction coinbase = createCoinBase(parent.getHeight() + 1);
        List<Transaction> transactionList = getTransactionPool().pop(-1);
        transactionList.add(0, coinbase);
        for (Transaction tx: transactionList) {
            // try to fetch transaction from pool
            try {
                Set<HexBytes> keys = updater.getRelatedKeys(tx, tmp.asMap());
                Map<HexBytes, Account> related = new HashMap<>();

                // get all account related to this transaction in the trie
                keys.forEach(k -> {
                    related.put(k, tmp.get(k).orElse(updater.createEmpty(k)));
                });

                // store updated result to the trie if update success
                updater
                        .update(related, header, tx)
                        .forEach(tmp::put);
            } catch (Exception e) {
                // prompt reason for failed updates
                log.error("execute transaction " + tx.getHash() + " failed, reason = " + e.getMessage());
                continue;
            }
            b.getBody().add(tx);
        }

        // calculate state root
        b.setStateRoot(
                HexBytes.fromBytes(tmp.commit())
        );

        // persist modifications of trie to database
        tmp.flush();
        b.resetTransactionsRoot();

        // the mined block cannot be modified any more
        return UnmodifiableBlock.of(b);
    }
}
