package org.tdf.sunflower.consensus;

import lombok.extern.slf4j.Slf4j;
import org.tdf.common.store.CachedStore;
import org.tdf.common.store.Store;
import org.tdf.common.trie.Trie;
import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.consensus.poa.PoAConstants;
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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Slf4j
public abstract class AbstractMiner implements Miner {
    protected abstract StateTrie<HexBytes, Account> getAccountTrie();

    protected abstract TransactionPool getTransactionPool();

    protected abstract Transaction createCoinBase(long height);

    protected Block createBlock(Block parent){
        Header header = Header.builder()
                .version(parent.getVersion())
                .hashPrev(parent.getHash()).height(parent.getHeight() + 1)
                .createdAt(System.currentTimeMillis() / 1000)
                .payload(PoAConstants.ZERO_BYTES)
                .build();

        org.tdf.sunflower.types.Block b = new org.tdf.sunflower.types.Block(header);

        Store<byte[], byte[]> cache = new CachedStore<>(getAccountTrie().getTrieStore(), ByteArrayMap::new);
        Trie<HexBytes, Account> tmp = getAccountTrie()
                .getTrie()
                .revert(parent.getStateRoot().getBytes(), cache);

        StateUpdater<HexBytes, Account> updater = getAccountTrie().getUpdater();
        while (true) {
            Optional<Transaction> o = getTransactionPool().pop();
            if (!o.isPresent()) break;
            Transaction tx = o.get();
            try {
                Set<HexBytes> keys = updater.getRelatedKeys(tx, tmp.asMap());
                Map<HexBytes, Account> related = new HashMap<>();
                keys.forEach(k -> {
                    related.put(k, tmp.get(k).orElse(updater.createEmpty(k)));
                });
                updater
                        .update(related, header, tx)
                        .forEach(tmp::put);
            } catch (Exception e) {
                log.error("execute transaction " + tx.getHash() + " failed, reason = " + e.getMessage());
                continue;
            }
            b.getBody().add(tx);
        }
        b.getBody().add(0, createCoinBase(parent.getHeight() + 1));

        // calculate state root
        b.setStateRoot(
                HexBytes.fromBytes(tmp.commit())
        );
        tmp.flush();
        b.resetTransactionsRoot();
        return UnmodifiableBlock.of(b);
    }
}
