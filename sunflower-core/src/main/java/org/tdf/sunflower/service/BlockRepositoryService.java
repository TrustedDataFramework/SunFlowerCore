package org.tdf.sunflower.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.tdf.common.util.ChainCache;
import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.dao.HeaderDao;
import org.tdf.sunflower.dao.Mapping;
import org.tdf.sunflower.dao.TransactionDao;
import org.tdf.sunflower.entity.TransactionEntity;
import org.tdf.sunflower.exception.ApplicationException;
import org.tdf.sunflower.exception.GenesisConflictsException;
import org.tdf.sunflower.exception.WriteGenesisFailedException;
import org.tdf.sunflower.facade.BlockRepository;
import org.tdf.sunflower.types.Block;
import org.tdf.sunflower.types.Header;
import org.tdf.sunflower.types.UnmodifiableBlock;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class BlockRepositoryService implements BlockRepository {
    @Autowired
    private HeaderDao headerDao;

    @Autowired
    private TransactionRepositoryService transactionRepositoryService;

    @Autowired
    private TransactionDao transactionDao;

    private Block genesis;

    private Block getBlockFromHeader(Header header) {
        Block b = new Block(header);
        b.setBody(
                transactionRepositoryService.getTransactionsByBlockHash(b.getHash().getBytes())
        );
        return UnmodifiableBlock.of(b);
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
        return blocks
                .stream()
                .map(UnmodifiableBlock::of)
                .collect(Collectors.toList());
    }

    @Override
    public void saveGenesis(Block block) throws GenesisConflictsException, WriteGenesisFailedException {
        this.genesis = block;
        Optional<Block> o = getBlockByHeight(0);
        if (!o.isPresent()) {
            writeBlock(genesis);
            return;
        }
        if (!o.get().getHash().equals(block.getHash())) {
            throw new GenesisConflictsException("genesis in db not equals to genesis in configuration");
        }
    }

    @Override
    public Block getGenesis() {
        return genesis;
    }

    @Override
    public boolean containsBlock(byte[] hash) {
        return headerDao.existsById(hash);
    }

    @Override
    public Header getBestHeader() {
        return Mapping.getFromHeaderEntity(headerDao.findTopByOrderByHeightDesc().get());
    }

    @Override
    public Block getBestBlock() {
        return getBlockFromHeader(getBestHeader());
    }

    @Override
    public Optional<Header> getHeader(byte[] hash) {
        return headerDao.findById(hash).map(Mapping::getFromHeaderEntity);
    }

    @Override
    public Optional<Block> getBlock(byte[] hash) {
        return getHeader(hash).map(this::getBlockFromHeader);
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
        return getHeaderByHeight(height).map(this::getBlockFromHeader);
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
                .map(ChainCache::of)
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
                .map(ChainCache::of)
                .map(c -> c.getAncestors(hash))
                .orElse(new ArrayList<>());
    }

    @Override
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    public void writeBlock(Block block) {
        headerDao.save(Mapping.getEntityFromHeader(block.getHeader()));

        List<TransactionEntity> entities =
                Mapping.getTransactionEntitiesFromBlock(block).collect(Collectors.toList());
        for(TransactionEntity e: entities){
            if(transactionDao.existsById(e.getHash()))
                throw new ApplicationException("transaction " + HexBytes.fromBytes(e.getHash()) + " already exists in database");
        }
        transactionDao.saveAll(entities);
    }

    @Override
    public void writeHeader(Header header) {
        headerDao.save(Mapping.getEntityFromHeader(header));
    }
}
