package org.tdf.sunflower.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.tdf.common.serialize.Codec;
import org.tdf.common.serialize.Codecs;
import org.tdf.common.store.Store;
import org.tdf.common.store.StoreWrapper;
import org.tdf.common.types.BlockConfirms;
import org.tdf.common.util.FastByteComparisons;
import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.ApplicationConstants;
import org.tdf.sunflower.events.NewBestBlock;
import org.tdf.sunflower.events.NewBlockWritten;
import org.tdf.sunflower.events.TransactionConfirmed;
import org.tdf.sunflower.exception.ApplicationException;
import org.tdf.sunflower.exception.GenesisConflictsException;
import org.tdf.sunflower.exception.WriteGenesisFailedException;
import org.tdf.sunflower.facade.ConfirmedBlocksProvider;
import org.tdf.sunflower.facade.SunflowerRepository;
import org.tdf.sunflower.types.Block;
import org.tdf.sunflower.types.Header;
import org.tdf.sunflower.types.Transaction;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j(topic = "db")
public class SunflowerRepositoryKVImpl extends AbstractBlockRepository implements SunflowerRepository {
    private static final String BEST_HEADER = "best";

    private static final String PRUNE = "prune";

    // transaction hash -> transaction
    private final Store<byte[], Transaction> transactionsStore;

    // block hash -> header
    private final Store<byte[], Header> headerStore;

    // transactions root -> transaction hashes
    private final Store<byte[], HexBytes[]> transactionsRoot;

    // block height -> block hashes
    private final Store<Long, HexBytes[]> heightIndex;

    // block height -> canonical hash
    private final Store<Long, byte[]> canonicalIndex;

    // "best" -> best header, "prune" -> pruned header
    private final Store<String, Header> status;

    // transaction hash -> block hashes which includes this transaction
    private final Store<byte[], HexBytes[]> transactionIncludes;

    private Header pruned;

    public SunflowerRepositoryKVImpl(ApplicationContext context) {
        super(context);
        this.transactionsStore = new StoreWrapper<>(
                factory.create("transactions"),
                Codec.identity(),
                Codecs.newRLPCodec(Transaction.class)
        );
        this.headerStore = new StoreWrapper<>(
                factory.create("headers"),
                Codec.identity(),
                Codecs.newRLPCodec(Header.class)
        );
        this.transactionsRoot = new StoreWrapper<>(
                factory.create("transactions-root"),
                Codec.identity(),
                Codecs.newRLPCodec(HexBytes[].class));
        this.heightIndex = new StoreWrapper<>(
                factory.create("height-index"),
                Codecs.newRLPCodec(Long.class),
                Codecs.newRLPCodec(HexBytes[].class)
        );
        this.status = new StoreWrapper<>(
                factory.create("block-store-status"),
                Codecs.STRING,
                Codecs.newRLPCodec(Header.class)
        );
        this.canonicalIndex = new StoreWrapper<>(
                factory.create("canonical-index"),
                Codecs.newRLPCodec(Long.class),
                Codec.identity()
        );
        this.transactionIncludes = new StoreWrapper<>(
                factory.create("transaction-includes"),
                Codec.identity(),
                Codecs.newRLPCodec(HexBytes[].class)
        );
    }


    @Override
    public void saveGenesis(Block block) throws GenesisConflictsException, WriteGenesisFailedException {
        super.saveGenesis(block);
        if (status.get(BEST_HEADER) == null) {
            status.put(BEST_HEADER, block.getHeader());
        }
    }

    @Override
    protected Block getBlockFromHeader(Header header) {
        HexBytes[] txHashes = transactionsRoot
                .get(header.getTransactionsRoot().getBytes());
        if (txHashes == null)
            throw new ApplicationException("transactions of header " + header + " not found");
        List<Transaction> body = new ArrayList<>(txHashes.length);
        for (HexBytes hash : txHashes) {
            Transaction t = transactionsStore.get(hash.getBytes());
            if (t == null)
                throw new ApplicationException("transaction " + hash + " not found");
            body.add(t);
        }
        Block ret = new Block(header);
        ret.setBody(body);
        return ret;
    }

    @Override
    protected List<Block> getBlocksFromHeaders(Collection<? extends Header> headers) {
        return headers.stream().map(this::getBlockFromHeader).collect(Collectors.toList());
    }

    @Override
    public boolean containsHeader(byte[] hash) {
        return headerStore.get(hash) != null;
    }

    @Override
    public Header getBestHeader() {
        return status.get(BEST_HEADER);
    }

    @Override
    public Optional<Header> getHeader(byte[] hash) {
        return Optional.ofNullable(headerStore.get(hash));
    }


    @Override
    public List<Header> getHeadersBetween(long startHeight, long stopHeight, int limit, boolean descend) {
        List<Header> ret = new ArrayList<>();
        if (descend) {
            for (long i = stopHeight; i >= startHeight; i--) {
                HexBytes[] idx = heightIndex.get(i);
                if (idx == null)
                    continue;

                for (HexBytes bytes : idx) {
                    Header h = headerStore.get(bytes.getBytes());
                    if (h == null) continue;
                    ret.add(h);
                }

                if (ret.size() > limit) break;
            }
        } else {
            for (long i = startHeight; i <= stopHeight; i++) {
                HexBytes[] idx = heightIndex.get(i);
                if (idx == null) continue;

                for (HexBytes bytes : idx) {
                    Header h = headerStore.get(bytes.getBytes());
                    if (h == null) continue;
                    ret.add(h);
                }
                if (ret.size() > limit) break;
            }
        }
        return ret;
    }


    @Override
    public List<Header> getHeadersByHeight(long height) {
        HexBytes[] idx = heightIndex.get(height);
        if (idx == null)
            return Collections.emptyList();
        List<Header> ret = new ArrayList<>(idx.length);
        for (HexBytes bytes : idx) {
            Header h = headerStore.get(bytes.getBytes());
            if (h == null)
                continue;

            ret.add(h);
        }
        return ret;
    }


    @Override
    public void writeBlock(Block block) {
        writeBlockNoReset(block);
        Block best = getBestBlock();
        if (Block.BEST_COMPARATOR.compare(best, block) < 0) {
            status.put(BEST_HEADER, block.getHeader());
            byte[] hash = block.getHash().getBytes();
            while (true) {
                Header o = headerStore.get(hash);
                if (o == null)
                    break;
                byte[] canonicalHash = canonicalIndex.get(o.getHeight());
                if (canonicalHash != null && canonicalHash.length != 0 && FastByteComparisons.equal(canonicalHash, hash))
                    break;
                canonicalIndex.put(o.getHeight(), hash);
                hash = o.getHashPrev().getBytes();
            }
            eventBus.publish(new NewBestBlock(block));
            long h = block.getHeight() - ApplicationConstants.TRANSACTION_CONFIRMS;
            if (h > 0) {
                getCanonicalBlock(h).get().getBody()
                        .stream().skip(1)
                        .forEach(tx -> eventBus.publish(new TransactionConfirmed(tx.getHash())));
            }
        }
    }

    @Override
    public void setProvider(ConfirmedBlocksProvider provider) {

    }

    @Override
    public boolean containsTransaction(byte[] hash) {
        return transactionsStore.get(hash) != null;
    }

    @Override
    public Optional<Transaction> getTransactionByHash(byte[] hash) {
        return Optional.ofNullable(transactionsStore.get(hash));
    }

    @Override
    public List<Transaction> getTransactionsByBlockHash(byte[] blockHash) {
        return getBlock(blockHash).map(Block::getBody).orElse(Collections.emptyList());
    }

    @Override
    public BlockConfirms getConfirms(byte[] transactionHash) {
        HexBytes[] blockHashes =
                transactionIncludes.get(transactionHash);

        if (blockHashes == null)
            blockHashes = new HexBytes[0];

        // 确认数
        if (blockHashes.length == 0)
            return new BlockConfirms(-1, null, null);
        for (HexBytes hash : blockHashes) {
            Header h = headerStore.get(hash.getBytes());
            if (h == null) continue;
            if (isCanonical(hash.getBytes()))
                return new BlockConfirms(getBestHeader().getHeight() - h.getHeight(), h.getHash(), h.getHeight());
        }
        return new BlockConfirms(-1, null, null);
    }

    private boolean isCanonical(Header h) {
        byte[] hash = canonicalIndex.get(h.getHeight());
        if (hash == null || hash.length == 0)
            return false;

        return FastByteComparisons.equal(hash, h.getHash().getBytes());
    }

    private boolean isCanonical(byte[] hash) {
        Header h = headerStore.get(hash);
        if (h == null)
            return false;
        return isCanonical(h);
    }

    @Override
    protected void writeGenesis(Block genesis) {
        writeBlockNoReset(genesis);
        canonicalIndex.put(0L, genesis.getHash().getBytes());
    }

    private void writeBlockNoReset(Block block) {
        byte[] v = accountTrie.getTrieStore().get(block.getStateRoot().getBytes());

        if (!block.getStateRoot().equals(HexBytes.fromBytes(accountTrie.getTrie().getNullHash()))
                && Store.IS_NULL.test(v)
        ) {
            throw new RuntimeException("unexpected error: account trie " + block.getStateRoot() + " not synced");
        }
        if (containsHeader(block.getHash().getBytes()))
            return;
        headerStore.put(block.getHash().getBytes(), block.getHeader());


        for (Transaction t : block.getBody()) {
            transactionsStore.put(t.getHash().getBytes(), t);
            Set<HexBytes> headerHashes = Optional.ofNullable(transactionIncludes.get(t.getHash().getBytes()))
                    .map(x -> new HashSet<>(Arrays.asList(x)))
                    .orElse(new HashSet<>());
            headerHashes.add(block.getHash());
            transactionIncludes.put(t.getHash().getBytes(), headerHashes.toArray(new HexBytes[0]));
        }


        HexBytes[] txHashes = block.getBody().stream().map(Transaction::getHash)
                .toArray(HexBytes[]::new);
        transactionsRoot.put(
                block.getTransactionsRoot().getBytes(),
                txHashes
        );
        Set<HexBytes> headerHashes = Optional.ofNullable(heightIndex.get(block.getHeight()))
                .map(x -> new HashSet<>(Arrays.asList(x)))
                .orElse(new HashSet<>());
        headerHashes.add(block.getHash());
        heightIndex.put(block.getHeight(), headerHashes.toArray(new HexBytes[0]));


        eventBus.publish(new NewBlockWritten(block));
        log.info("write block at height " + block.getHeight() + " " + block.getHeader().getHash() + " to database success");
    }

//    @Override
//    public void prune(byte[] hash) {
//        Block pruned = getBlock(hash).orElseThrow(
//                () -> new RuntimeException("pruned " + HexBytes.fromBytes(hash) + " not found")
//        );
//
//        Optional<Header> lastPruned = status.get(PRUNE);
//        Header best = getBestHeader();
//
//
//        if (lastPruned.isPresent() && lastPruned.get().getHash().equals(pruned.getHash())) {
//            this.pruned = lastPruned.get();
//            return;
//        }
//
//        // try to find ancestor of current best pruned
//        Header ancestor = best;
//
//        while (ancestor.getHeight() > pruned.getHeight()) {
//            ancestor = headerStore.get(ancestor.getHashPrev().getBytes()).get();
//        }
//
//        if (!ancestor.getHash().equals(pruned.getHash()))
//            throw new RuntimeException("the pruned block " + pruned.getHash() + " is not canonical");
//
//        Set<HexBytes> txWhiteList = new HashSet<>();
//        Set<HexBytes> txRootWhiteList = new HashSet<>();
//        Set<byte[]> stateRootWhiteList = new ByteArraySet();
//        stateRootWhiteList.add(genesis.getStateRoot().getBytes());
//
//        BiConsumer<byte[], Header> fn = (k, h) -> {
//            if (h.getHeight() <= pruned.getHeight() && h.getHeight() != 0 && !h.getHash().equals(pruned.getHash())) {
//                headerStore.remove(k);
//                heightIndex.remove(h.getHeight());
//            } else {
//                HexBytes[] txs = transactionsRoot.get(h.getTransactionsRoot().getBytes()).orElse(new HexBytes[0]);
//                txWhiteList.addAll(Arrays.asList(txs));
//                txRootWhiteList.add(h.getTransactionsRoot());
//                stateRootWhiteList.add(h.getStateRoot().getBytes());
//            }
//        };
//
//        if (((StoreWrapper) headerStore).getStore() instanceof MemoryDatabaseStore) {
//            headerStore.stream().collect(Collectors.toList()).forEach(e -> fn.accept(e.getKey(), e.getValue()));
//        } else {
//            headerStore.forEach(fn);
//        }
//
//        for (Store<byte[], ?> store : Arrays.asList(transactionsStore, transactionsRoot, transactionIncludes)) {
//            BiConsumer<byte[], Object> f = (k, v) -> {
//                if (!txWhiteList.contains(HexBytes.fromBytes(k)))
//                    store.remove(k);
//            };
//            if (((StoreWrapper) store).getStore() instanceof MemoryDatabaseStore) {
//                transactionsStore
//                        .stream()
//                        .collect(Collectors.toList())
//                        .forEach(e -> f.accept(e.getKey(), e.getValue()));
//            } else {
//                transactionsStore.forEach(f);
//            }
//        }
//
//
//        this.pruned = pruned.getHeader();
//        accountTrie.prune(stateRootWhiteList);
//
//        Set<byte[]> storageRootWhiteList = new ByteArraySet();
//        for (byte[] bytes : stateRootWhiteList) {
//            accountTrie.getTrie(bytes).traverse(e -> {
//                Account v = e.getValue();
//                if (v.getStorageRoot() != null
//                        && v.getStorageRoot().length != 0
//                        && !FastByteComparisons.equal(contractStorageTrie.getNullHash(), v.getStorageRoot())
//                ) {
//                    storageRootWhiteList.add(v.getStorageRoot());
//                }
//                return true;
//            });
//        }
//        contractStorageTrie.prune(storageRootWhiteList);
//        status.put(PRUNE, pruned.getHeader());
//    }

//    @Override
//    public long getPrunedHeight() {
//        return pruned == null ? 0 : pruned.getHeight();
//    }
//
//    @Override
//    public HexBytes getPrunedHash() {
//        return pruned == null ? null : pruned.getHash();
//    }

    @Override
    public Optional<Header> getCanonicalHeader(long height) {
        byte[] v = canonicalIndex.get(height);
        if (v == null || v.length == 0)
            return Optional.empty();
        return getHeader(v);
    }
//
//    @Override
//    public void traverseTransactions(BiFunction<byte[], Transaction, Boolean> traverser) {
//        transactionsStore.traverse(traverser);
//    }
}
