package org.tdf.sunflower.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.tdf.common.util.ChainCache;
import org.tdf.common.util.ChainCacheImpl;
import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.dao.BlockDao;
import org.tdf.sunflower.dao.HeaderDao;
import org.tdf.sunflower.dao.Mapping;
import org.tdf.sunflower.dao.TransactionDao;
import org.tdf.sunflower.entity.TransactionEntity;
import org.tdf.sunflower.exception.GenesisConflictsException;
import org.tdf.sunflower.exception.WriteGenesisFailedException;
import org.tdf.sunflower.facade.BlockRepository;
import org.tdf.sunflower.types.Block;
import org.tdf.sunflower.types.Header;

import javax.transaction.Transactional;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class BlockRepositoryService implements BlockRepository {
    @Autowired
    private BlockDao blockDao;

    @Autowired
    private HeaderDao headerDao;

    @Autowired
    private TransactionDao transactionDao;

    private Block genesis;

    private Block getBlockFromHeader(Header header) {
        Block b = new Block(header);
        b.setBody(
                transactionDao.findByBlockHashOrderByPosition(b.getHash().getBytes(), PageRequest.of(0, Integer.MAX_VALUE))
                        .stream().map(Mapping::getFromTransactionEntity).collect(Collectors.toList())
        );
        return b;
    }

    private List<Block> getBlocksFromHeaders(Collection<Header> headers) {
        List<TransactionEntity> transactions = transactionDao.findByBlockHashIn(
                headers.stream().map(h -> h.getHash().getBytes()).collect(Collectors.toList())
        );

        Map<String, List<TransactionEntity>> transactionLists = new HashMap<>();
        transactions.forEach(t -> {
            String key = HexBytes.encode(t.getBlockHash());
            transactionLists.putIfAbsent(HexBytes.encode(t.getBlockHash()), new ArrayList<>());
            transactionLists.get(key).add(t);
        });
        List<Block> blocks = headers.stream().map(Block::new).collect(Collectors.toList());

        for (Block b : blocks) {
            List<TransactionEntity> list = transactionLists.get(b.getHash().toString());
            if (list == null) {
                continue;
            }
            list.sort((x, y) -> x.getPosition() - y.getPosition());
            b.setBody(list.stream().map(Mapping::getFromTransactionEntity).collect(Collectors.toList()));
        }
        return blocks;
    }

    @Override
    public void saveGenesisBlock(Block block) throws GenesisConflictsException, WriteGenesisFailedException {
        this.genesis = block;
        Optional<Block> o = getBlockByHeight(0);
        if (!o.isPresent()){
            writeBlock(genesis);
            return;
        }
        if (!o.get().getHash().equals(block.getHash())){
            throw new GenesisConflictsException("genesis in db not equals to genesis in configuration");
        }
    }

    @Override
    public Block getGenesis() {
        return genesis;
    }

    @Override
    public boolean hasBlock(byte[] hash) {
        return headerDao.existsById(hash);
    }

    @Override
    public Header getBestHeader() {
        return Mapping.getFromHeaderEntity(headerDao.findTopByOrderByHeightDesc().get());
    }

    @Override
    public Block getBestBlock() {
        return Mapping.getFromBlockEntity(blockDao.findTopByOrderByHeightDesc().get());
    }

    @Override
    public Optional<Header> getHeader(byte[] hash) {
        return headerDao.findById(hash).map(Mapping::getFromHeaderEntity);
    }

    @Override
    public Optional<Block> getBlock(byte[] hash) {
        return blockDao.findById(hash).map(Mapping::getFromBlockEntity);
    }

    @Override
    public List<Header> getHeaders(long startHeight, int limit) {
        if (limit == 0) return new ArrayList<>();
        if (limit < 0) limit = Integer.MAX_VALUE;
        return Mapping.getFromHeaderEntities(headerDao
                .findByHeightGreaterThanEqual(startHeight, PageRequest.of(0, limit)));
    }

    @Override
    public List<Block> getBlocks(long startHeight, int limit) {
        return getBlocksFromHeaders(getHeaders(startHeight, limit));
    }

    @Override
    public List<Header> getHeadersBetween(long startHeight, long stopHeight) {
        return headerDao.findByHeightBetweenOrderByHeight(startHeight, stopHeight).stream()
                .map(Mapping::getFromHeaderEntity).collect(Collectors.toList());
    }

    @Override
    public List<Block> getBlocksBetween(long startHeight, long stopHeight) {
        return getBlocksFromHeaders(getHeadersBetween(startHeight, stopHeight));
    }

    @Override
    public List<Header> getHeadersBetween(long startHeight, long stopHeight, int limit) {
        if (limit == 0) return new ArrayList<>();
        if (limit < 0) limit = Integer.MAX_VALUE;
        return Mapping.getFromHeaderEntities(
                headerDao.findByHeightBetweenOrderByHeightAsc(
                        startHeight, stopHeight, PageRequest.of(0, limit)
                )
        );
    }

    @Override
    public List<Header> getHeadersBetweenDescend(long startHeight, long stopHeight, int limit) {
        return Mapping.getFromHeaderEntities(
                headerDao.findByHeightBetweenOrderByHeightDesc(
                        startHeight, stopHeight, PageRequest.of(0, limit)
                )
        );
    }

    @Override
    public List<Block> getBlocksBetween(long startHeight, long stopHeight, int limit) {
        return getBlocksFromHeaders(getHeadersBetween(startHeight, stopHeight, limit));
    }

    @Override
    public List<Block> getBlocksBetweenDescend(long startHeight, long stopHeight, int limit) {
        return getBlocksFromHeaders(getHeadersBetweenDescend(startHeight, stopHeight, limit));
    }

    @Override
    public Optional<Header> getHeaderByHeight(long height) {
        return headerDao.findByHeight(height).map(Mapping::getFromHeaderEntity);
    }

    @Override
    public Optional<Block> getBlockByHeight(long height) {
        return blockDao.findByHeight(height).map(Mapping::getFromBlockEntity);
    }

    @Override
    public Optional<Header> getAncestorHeader(byte[] hash, long ancestorHeight) {
        Optional<Header> header = getHeader(hash);
        return header.map(h -> getHeadersBetween(ancestorHeight, h.getHeight()))
                .map(ChainCacheImpl::of)
                .map(c -> c.getAncestors(hash))
                .flatMap(li -> li.stream().filter(x -> x.getHeight() == ancestorHeight).findFirst())
                ;
    }

    @Override
    public Optional<Block> getAncestorBlock(byte[] hash, long ancestorHeight) {
        Optional<Header> header = getAncestorHeader(hash, ancestorHeight);
        return header.map(this::getBlockFromHeader);
    }

    @Override
    public List<Header> getAncestorHeaders(byte[] hash, int limit) {
        if (limit == 0) return new ArrayList<>();
        if (limit < 0) limit = Integer.MAX_VALUE;
        Optional<Header> header = getHeader(hash);
        int finalLimit = limit;
        return header.map(h ->
                    getHeadersBetween(
                            header.get().getHeight() - finalLimit + 1, h.getHeight(), finalLimit)
                    )
                .map(ChainCacheImpl::of)
                .map(c -> c.getAncestors(hash))
                .orElse(new ArrayList<>());
    }

    @Override
    public List<Block> getAncestorBlocks(byte[] hash, int limit) {
        if (limit == 0) return new ArrayList<>();
        if (limit < 0) limit = Integer.MAX_VALUE;
        Optional<Block> block = getBlock(hash);
        int finalLimit = limit;
        return block.map(h ->
                getBlocksBetweenDescend(
                        block.get().getHeight() - finalLimit + 1, h.getHeight(), finalLimit)
                )
                .map(ChainCacheImpl::of)
                .map(c -> c.getAncestors(hash))
                .orElse(new ArrayList<>());
    }

    @Override
    @Transactional
    public boolean writeBlock(Block block) {
        return blockDao.save(Mapping.getEntityFromBlock(block)) != null;
    }
}
