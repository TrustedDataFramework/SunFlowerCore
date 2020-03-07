package org.tdf.sunflower.service;

import lombok.RequiredArgsConstructor;
import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.exception.GenesisConflictsException;
import org.tdf.sunflower.exception.WriteGenesisFailedException;
import org.tdf.sunflower.facade.ConfirmedBlocksProvider;
import org.tdf.sunflower.facade.SunflowerRepository;
import org.tdf.sunflower.state.Account;
import org.tdf.sunflower.state.StateTrie;
import org.tdf.sunflower.types.Block;
import org.tdf.sunflower.types.Header;
import org.tdf.sunflower.types.Transaction;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@RequiredArgsConstructor
public class ConcurrentSunflowerRepository implements SunflowerRepository {
    private final SunflowerRepository delegate;
    private ReadWriteLock lock = new ReentrantReadWriteLock();

    @Override
    public void setAccountTrie(StateTrie<HexBytes, Account> accountTrie) {
        delegate.setAccountTrie(accountTrie);
    }

    @Override
    public Block getGenesis() {
        lock.readLock().lock();
        try {
            return delegate.getGenesis();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void saveGenesis(Block block) throws GenesisConflictsException, WriteGenesisFailedException {
        lock.writeLock().lock();
        try {
            delegate.saveGenesis(block);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public boolean containsHeader(byte[] hash) {
        lock.readLock().lock();
        try {
            return delegate.containsHeader(hash);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Header getBestHeader() {
        lock.readLock().lock();
        try {
            return delegate.getBestHeader();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Block getBestBlock() {
        lock.readLock().lock();
        try {
            return delegate.getBestBlock();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Optional<Header> getHeader(byte[] hash) {
        lock.readLock().lock();
        try {
            return delegate.getHeader(hash);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Optional<Block> getBlock(byte[] hash) {
        lock.readLock().lock();
        try {
            return delegate.getBlock(hash);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public List<Header> getHeadersBetween(long startHeight, long stopHeight) {
        lock.readLock().lock();
        try {
            return delegate.getHeadersBetween(startHeight, stopHeight);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public List<Block> getBlocksBetween(long startHeight, long stopHeight) {
        lock.readLock().lock();
        try {
            return delegate.getBlocksBetween(startHeight, stopHeight);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public List<Header> getHeadersBetween(long startHeight, long stopHeight, int limit) {
        lock.readLock().lock();
        try {
            return delegate.getHeadersBetween(startHeight, stopHeight, limit);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public List<Header> getHeadersBetween(long startHeight, long stopHeight, int limit, boolean descend) {
        lock.readLock().lock();
        try {
            return delegate.getHeadersBetween(startHeight, stopHeight, limit, descend);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public List<Block> getBlocksBetween(long startHeight, long stopHeight, int limit) {
        lock.readLock().lock();
        try {
            return delegate.getBlocksBetween(startHeight, stopHeight, limit);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public List<Block> getBlocksBetween(long startHeight, long stopHeight, int limit, boolean descend) {
        lock.readLock().lock();
        try {
            return delegate.getBlocksBetween(startHeight, stopHeight, limit, descend);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public List<Header> getHeadersByHeight(long height) {
        lock.readLock().lock();
        try {
            return delegate.getHeadersByHeight(height);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public List<Block> getBlocksByHeight(long height) {
        lock.readLock().lock();
        try {
            return delegate.getBlocksByHeight(height);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Optional<Block> getCanonicalBlock(long height) {
        lock.readLock().lock();
        try {
            return delegate.getCanonicalBlock(height);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Optional<Header> getCanonicalHeader(long height) {
        lock.readLock().lock();
        try {
            return delegate.getCanonicalHeader(height);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void writeBlock(Block block) {
        lock.writeLock().lock();
        try {
            delegate.writeBlock(block);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public boolean containsTransaction(byte[] hash) {
        lock.readLock().lock();
        try {
            return delegate.containsTransaction(hash);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Optional<Transaction> getTransactionByHash(byte[] hash) {
        lock.readLock().lock();
        try {
            return delegate.getTransactionByHash(hash);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public List<Transaction> getTransactionsByBlockHash(byte[] blockHash) {
        lock.readLock().lock();
        try {
            return delegate.getTransactionsByBlockHash(blockHash);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void setProvider(ConfirmedBlocksProvider provider) {

    }

    @Override
    public void prune(byte[] hash) {
        lock.writeLock().lock();
        try {
            delegate.prune(hash);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public long getPrunedHeight() {
        return delegate.getPrunedHeight();
    }

    @Override
    public HexBytes getPrunedHash() {
        return delegate.getPrunedHash();
    }
}
