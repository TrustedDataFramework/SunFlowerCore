package org.tdf.sunflower.service;

import lombok.Setter;
import org.springframework.context.ApplicationContext;
import org.tdf.common.event.EventBus;
import org.tdf.common.store.Store;
import org.tdf.common.trie.Trie;
import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.exception.GenesisConflictsException;
import org.tdf.sunflower.exception.WriteGenesisFailedException;
import org.tdf.sunflower.facade.BlockRepository;
import org.tdf.sunflower.facade.DatabaseStoreFactory;
import org.tdf.sunflower.state.Account;
import org.tdf.sunflower.state.StateTrie;
import org.tdf.sunflower.types.Block;
import org.tdf.sunflower.types.Header;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public abstract class AbstractBlockRepository implements BlockRepository {
    protected final EventBus eventBus;
    protected final DatabaseStoreFactory factory;
    protected final Trie<byte[], byte[]> contractStorageTrie;
    protected final Store<byte[], byte[]> contractCodeStore;
    protected Block genesis;
    @Setter
    protected StateTrie<HexBytes, Account> accountTrie;

    public AbstractBlockRepository(ApplicationContext context) {
        this.eventBus = context.getBean(EventBus.class);
        this.factory = context.getBean(DatabaseStoreFactory.class);
        this.contractStorageTrie = context.getBean("contractStorageTrie", Trie.class);
        this.contractCodeStore = context.getBean("contractCodeStore", Store.class);
    }

    @Override
    public Block getGenesis() {
        return genesis;
    }

    protected abstract void writeGenesis(Block genesis);

    @Override
    public long getPrunedHeight() {
        return 0;
    }

    @Override
    public HexBytes getPrunedHash() {
        return null;
    }


    @Override
    public void saveGenesis(Block block) throws GenesisConflictsException, WriteGenesisFailedException {
        this.genesis = block;
        List<Block> o = getBlocksByHeight(0);
        if (o.isEmpty()) {
            writeGenesis(genesis);
            return;
        }
        if (o.size() > 1 || o.stream().anyMatch(x -> !x.getHash().equals(block.getHash()))) {
            throw new GenesisConflictsException("genesis in db not equals to genesis in configuration");
        }
    }

    protected abstract Block getBlockFromHeader(Header header);

    protected abstract List<Block> getBlocksFromHeaders(Collection<? extends Header> headers);

    @Override
    public Optional<Block> getCanonicalBlock(long height) {
        return getCanonicalHeader(height).map(this::getBlockFromHeader);
    }

    public Block getBestBlock() {
        return getBlockFromHeader(getBestHeader());
    }

    @Override
    public List<Block> getBlocksBetween(long startHeight, long stopHeight) {
        return getBlocksFromHeaders(getHeadersBetween(startHeight, stopHeight));
    }

    @Override
    public List<Block> getBlocksBetween(long startHeight, long stopHeight, int limit) {
        return getBlocksFromHeaders(getHeadersBetween(startHeight, stopHeight, limit));
    }

    @Override
    public List<Block> getBlocksBetween(long startHeight, long stopHeight, int limit, boolean descend) {
        return getBlocksFromHeaders(getHeadersBetween(startHeight, stopHeight, limit, descend));
    }

    @Override
    public List<Block> getBlocksByHeight(long height) {
        return getBlocksFromHeaders(getHeadersByHeight(height));
    }

    @Override
    public Optional<Block> getBlock(byte[] hash) {
        return getHeader(hash).map(this::getBlockFromHeader);
    }

    @Override
    public List<Header> getHeadersBetween(long startHeight, long stopHeight) {
        return getHeadersBetween(startHeight, stopHeight, Integer.MAX_VALUE);
    }

    @Override
    public List<Header> getHeadersBetween(long startHeight, long stopHeight, int limit) {
        return getHeadersBetween(startHeight, stopHeight, limit, false);
    }
}
