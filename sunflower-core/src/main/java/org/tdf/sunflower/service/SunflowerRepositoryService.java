package org.tdf.sunflower.service;

import lombok.Setter;
import lombok.experimental.Delegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.tdf.common.event.EventBus;
import org.tdf.common.util.ChainCache;
import org.tdf.common.util.ChainCacheImpl;
import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.events.NewBestBlock;
import org.tdf.sunflower.events.NewBlockWritten;
import org.tdf.sunflower.events.NewBlockConfirmed;
import org.tdf.sunflower.facade.*;
import org.tdf.sunflower.state.Account;
import org.tdf.sunflower.state.StateTrie;
import org.tdf.sunflower.types.Block;
import org.tdf.sunflower.types.Header;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

@Service
public class SunflowerRepositoryService implements SunflowerRepository {
    private abstract static class ExcludedMethods{
        public abstract void writeBlock(Block block);
    }

    @Autowired
    private EventBus eventBus;

    private ChainCache<Block> cache = new ChainCacheImpl<>();

    @Setter
    private StateTrie<HexBytes, Account> accountTrie;

    @Qualifier("blockRepositoryService")
    @Autowired
    @Delegate(excludes = ExcludedMethods.class)
    private BlockRepository blockRepository;

    @Qualifier("transactionRepositoryService")
    @Autowired
    @Delegate
    private TransactionRepository transactionRepository;

    @Override
    public Block getLastConfirmed() {
        return getBestBlock();
    }

    @Override
    public List<Block> getUnconfirmed() {
        return new ArrayList<>();
    }

    @Override
    public void setProvider(ConfirmedBlocksProvider provider) {

    }

    @Override
    public void writeBlock(Block block) {
        if(!accountTrie.getTrieStore().containsKey(block.getStateRoot().getBytes())){
            throw new RuntimeException("unexpected error: account trie not synced");
        }
        blockRepository.writeBlock(block);
        eventBus.publish(new NewBlockWritten(block));
        eventBus.publish(new NewBestBlock(block));
        eventBus.publish(new NewBlockConfirmed(block));
    }
}
