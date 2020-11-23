package org.tdf.sunflower.service;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.tdf.common.types.BlockConfirms;
import org.tdf.common.types.Chained;
import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.dao.HeaderDao;
import org.tdf.sunflower.dao.Mapping;
import org.tdf.sunflower.dao.TransactionDao;
import org.tdf.sunflower.entity.TransactionEntity;
import org.tdf.sunflower.exception.ApplicationException;
import org.tdf.sunflower.facade.ConfirmedBlocksProvider;
import org.tdf.sunflower.facade.SunflowerRepository;
import org.tdf.sunflower.types.Block;
import org.tdf.sunflower.types.Header;
import org.tdf.sunflower.types.Transaction;
import org.tdf.sunflower.types.UnmodifiableBlock;

import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;


@Slf4j(topic = "db")
public class SunflowerRepositoryService extends AbstractBlockRepository implements SunflowerRepository {
    private final HeaderDao headerDao;
    private final TransactionDao transactionDao;

    public SunflowerRepositoryService(
            ApplicationContext context
    ) {
        super(context);
        this.headerDao = context.getBean(HeaderDao.class);
        this.transactionDao = context.getBean(TransactionDao.class);
    }

    @Override
    public Optional<Header> getCanonicalHeader(long height) {
        return Optional.empty();
    }

    protected Block getBlockFromHeader(Header header) {
        Block b = new Block(header);
        b.setBody(
                getTransactionsByBlockHash(b.getHash().getBytes())
        );
        return UnmodifiableBlock.of(b);
    }

    protected List<Block> getBlocksFromHeaders(Collection<? extends Header> headers) {
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
    public boolean containsHeader(byte[] hash) {
        return headerDao.existsById(hash);
    }

    @Override
    public Header getBestHeader() {
        return Mapping.getFromHeaderEntity(headerDao.findTopByOrderByHeightDesc().get());
    }


    @Override
    public Optional<Header> getHeader(byte[] hash) {
        return headerDao.findById(hash).map(Mapping::getFromHeaderEntity);
    }


    public List<Header> getHeaders(long startHeight, int limit) {
        if (limit == 0) return new ArrayList<>();
        if (limit < 0) limit = Integer.MAX_VALUE;
        return Mapping.getFromHeaderEntities(headerDao
                .findByHeightGreaterThanEqual(startHeight, PageRequest.of(0, limit)));
    }


    @Override
    public List<Header> getHeadersBetween(long startHeight, long stopHeight, int limit, boolean descend) {
        return Mapping.getFromHeaderEntities(
                headerDao.findByHeightBetween(
                        startHeight, stopHeight,
                        PageRequest.of(0, limit,
                                descend ? Sort.Direction.DESC : Sort.Direction.ASC, "height"
                        )
                )
        );
    }

    @Override
    public List<Header> getHeadersByHeight(long height) {
        return headerDao.findByHeight(height).stream().map(Mapping::getFromHeaderEntity).collect(Collectors.toList());
    }


    public List<Header> getAncestorHeaders(byte[] hash, int limit) {
        if (limit == 0) return new ArrayList<>();
        if (limit < 0) limit = Integer.MAX_VALUE;
        Optional<Header> header = getHeader(hash);
        int finalLimit = limit;
        return header.map(h ->
                getHeadersBetween(
                        header.get().getHeight() - finalLimit + 1, h.getHeight(), finalLimit)
        )
                .map(li -> Chained.getAncestorsOf(li, HexBytes.fromBytes(hash)))
                .orElse(new ArrayList<>());
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void writeBlock(Block block) {
        if (!accountTrie.getTrieStore().containsKey(block.getStateRoot().getBytes())) {
            throw new RuntimeException("unexpected error: account trie not synced");
        }
        if (headerDao.existsById(block.getHash().getBytes()))
            throw new RuntimeException(block + " had been persisted");
        List<TransactionEntity> entities =
                Mapping.getTransactionEntitiesFromBlock(block).collect(Collectors.toList());

        if (!transactionDao
                .findAllById(block.getBody().stream().map(x -> x.getHash().getBytes()).collect(Collectors.toList()))
                .isEmpty()) {
            throw new ApplicationException("transaction already exists in database");
        }
        headerDao.save(Mapping.getEntityFromHeader(block.getHeader()));
        transactionDao.saveAll(entities);
        log.info("write block at height " + block.getHeight() + " " + block.getHeader().getHash() + " to database success");
    }

    public void writeHeader(Header header) {
        headerDao.save(Mapping.getEntityFromHeader(header));
    }

    @Override
    protected void writeGenesis(Block genesis) {
        writeBlock(genesis);
    }

    @Override
    public boolean containsTransaction(@NonNull byte[] hash) {
        return transactionDao.existsById(hash);
    }


    @Override
    public Optional<Transaction> getTransactionByHash(byte[] hash) {
        return transactionDao.findById(hash).map(Mapping::getFromTransactionEntity);
    }

    @Override
    public List<Transaction> getTransactionsByBlockHash(byte[] blockHash) {
        return transactionDao.findByBlockHash(blockHash)
                .stream().sorted(Comparator.comparingInt(TransactionEntity::getPosition))
                .map(Mapping::getFromTransactionEntity)
                .collect(Collectors.toList());
    }

    @Override
    public BlockConfirms getConfirms(byte[] transactionHash) {
        return null;
    }

    public List<Transaction> getTransactionsByBlockHeight(long height) {
        return transactionDao.findByHeight(height)
                .stream().sorted(Comparator.comparingInt(TransactionEntity::getPosition))
                .map(Mapping::getFromTransactionEntity)
                .collect(Collectors.toList());
    }

    @Override
    public void setProvider(ConfirmedBlocksProvider provider) {

    }

    @Override
    public void prune(byte[] hash) {

    }

    @Override
    public void traverseTransactions(BiFunction<byte[], Transaction, Boolean> traverser) {
        throw new UnsupportedOperationException();
    }
}
