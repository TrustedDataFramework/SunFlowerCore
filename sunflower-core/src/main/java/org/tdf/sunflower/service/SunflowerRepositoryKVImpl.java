package org.tdf.sunflower.service;

import lombok.extern.slf4j.Slf4j;
import org.tdf.common.event.EventBus;
import org.tdf.common.serialize.Codec;
import org.tdf.common.serialize.Codecs;
import org.tdf.common.store.Store;
import org.tdf.common.store.StoreWrapper;
import org.tdf.common.util.ByteArraySet;
import org.tdf.common.util.HexBytes;
import org.tdf.rlp.RLPCodec;
import org.tdf.sunflower.db.DatabaseStoreFactory;
import org.tdf.sunflower.events.NewBestBlock;
import org.tdf.sunflower.events.NewBlockWritten;
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

@Slf4j
public class SunflowerRepositoryKVImpl extends AbstractBlockRepository implements SunflowerRepository {
    private static final String BEST_HEADER = "best";

    private static final String PRUNE = "prune";

    // transaction hash -> transaction
    private Store<byte[], Transaction> transactionsStore;

    // block hash -> header
    private Store<byte[], Header> headerStore;

    // transactions root -> transaction hashes
    private Store<byte[], HexBytes[]> transactionsRoot;

    // block height -> block hashes
    private Store<Long, HexBytes[]> heightIndex;

    private Store<String, Header> status;

    private Block bestBlock;


    public SunflowerRepositoryKVImpl(EventBus eventBus, DatabaseStoreFactory factory) {
        super(eventBus);
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
    }


    @Override
    public void saveGenesis(Block block) throws GenesisConflictsException, WriteGenesisFailedException {
        super.saveGenesis(block);
        if (!status.get(BEST_HEADER).isPresent()) {
            status.put(BEST_HEADER, block.getHeader());
        }
    }

    @Override
    protected Block getBlockFromHeader(Header header) {
        HexBytes[] txHashes = transactionsRoot
                .get(header.getTransactionsRoot().getBytes())
                .orElseThrow(() -> new ApplicationException("transactions of header " + header + " not found"));
        List<Transaction> body = new ArrayList<>(txHashes.length);
        for (HexBytes hash : txHashes) {
            body.add(transactionsStore.get(hash.getBytes()).orElseThrow(
                    () -> new ApplicationException("transaction " + hash + " not found")
            ));
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
        return headerStore.containsKey(hash);
    }

    @Override
    public Header getBestHeader() {
        if (bestBlock != null) return bestBlock.getHeader();
        return status.get(BEST_HEADER).get();
    }


    @Override
    public Block getBestBlock() {
        if (bestBlock != null) return bestBlock;
        return super.getBestBlock();
    }

    @Override
    public Optional<Header> getHeader(byte[] hash) {
        return headerStore.get(hash);
    }


    @Override
    public List<Header> getHeadersBetween(long startHeight, long stopHeight, int limit, boolean descend) {
        List<Header> ret = new ArrayList<>();
        if (descend) {
            for (long i = stopHeight; i >= startHeight; i--) {
                heightIndex.get(i).ifPresent(hexBytes -> {
                    for (HexBytes bytes : hexBytes) {
                        headerStore.get(bytes.getBytes()).ifPresent(ret::add);
                    }
                });
                if (ret.size() > limit) break;
            }
        } else {
            for (long i = startHeight; i <= stopHeight; i++) {
                heightIndex.get(i).ifPresent(hexBytes -> {
                    for (HexBytes bytes : hexBytes) {
                        headerStore.get(bytes.getBytes()).ifPresent(ret::add);
                    }
                });
                if (ret.size() > limit) break;
            }
        }
        return ret;
    }


    @Override
    public List<Header> getHeadersByHeight(long height) {
        return heightIndex.get(height).map(hexBytes -> {
            List<Header> ret = new ArrayList<>(hexBytes.length);
            for (HexBytes bytes : hexBytes) {
                headerStore.get(bytes.getBytes()).ifPresent(ret::add);
            }
            return ret;
        }).orElse(Collections.emptyList());
    }


    @Override
    public void writeBlock(Block block) {
        writeBlockNoReset(block);
        Block best = getBestBlock();
        if (Block.BEST_COMPARATOR.compare(best, block) < 0) {
            status.put(BEST_HEADER, block.getHeader());
            this.bestBlock = block;
            eventBus.publish(new NewBestBlock(block));
        }
    }

    @Override
    public void setProvider(ConfirmedBlocksProvider provider) {

    }

    @Override
    public boolean containsTransaction(byte[] hash) {
        return transactionsStore.containsKey(hash);
    }

    @Override
    public Optional<Transaction> getTransactionByHash(byte[] hash) {
        return transactionsStore.get(hash);
    }

    @Override
    public List<Transaction> getTransactionsByBlockHash(byte[] blockHash) {
        return getBlock(blockHash).map(Block::getBody).orElse(Collections.emptyList());
    }

    @Override
    protected void writeGenesis(Block genesis) {
        writeBlockNoReset(genesis);
    }

    @Override
    public void writeHeader(Header header) {
        headerStore.put(header.getHash().getBytes(), header);
    }

    private void writeBlockNoReset(Block block) {
        if (!accountTrie.getTrieStore().containsKey(block.getStateRoot().getBytes())) {
            throw new RuntimeException("unexpected error: account trie not synced");
        }
        if (containsHeader(block.getHash().getBytes()))
            return;
        headerStore.put(block.getHash().getBytes(), block.getHeader());
        block.getBody()
                .forEach(t -> transactionsStore.put(t.getHash().getBytes(), t));
        HexBytes[] txHashes = block.getBody().stream().map(Transaction::getHash)
                .toArray(HexBytes[]::new);
        transactionsRoot.put(
                block.getTransactionsRoot().getBytes(),
                txHashes
        );
        Set<HexBytes> headerHashes = heightIndex.get(block.getHeight())
                .map(x -> new HashSet<>(Arrays.asList(x)))
                .orElse(new HashSet<>());
        headerHashes.add(block.getHash());
        heightIndex.put(block.getHeight(), headerHashes.toArray(new HexBytes[0]));

        eventBus.publish(new NewBlockWritten(block));
        log.info("write block at height " + block.getHeight() + " " + block.getHeader().getHash() + " to database success");
    }

    @Override
    public void prune(byte[] hash) {
        Header header = getHeader(hash).orElseThrow(
                () -> new RuntimeException("header " + HexBytes.fromBytes(hash) + " not found")
        );

        Optional<Header> lastPruned = status.get(PRUNE);
        Block best = getBestBlock();

        if(header.getHeight() >= best.getHeight())
            throw new RuntimeException("the pruned block height should less than current best block " + best);

        if(lastPruned.isPresent() && lastPruned.get().getHash().equals(header.getHash())){
            return;
        }

        Set<HexBytes> txWhiteList = new HashSet<>();
        Set<HexBytes> txRootWhiteList = new HashSet<>();
        Set<byte[]> stateRootWhiteList = new ByteArraySet();

        headerStore.forEach((k, h) -> {
            if (h.getHeight() <= header.getHeight() && h.getHeight() != 0 && !h.getHash().equals(header.getHash())) {
                headerStore.remove(k);
            } else {
                HexBytes[] txs = transactionsRoot.get(h.getTransactionsRoot().getBytes()).orElse(new HexBytes[0]);
                txWhiteList.addAll(Arrays.asList(txs));
                txRootWhiteList.add(h.getTransactionsRoot());
                stateRootWhiteList.add(h.getStateRoot().getBytes());
            }
        });

        transactionsRoot.forEach((k, v) -> {
            if (!txRootWhiteList.contains(HexBytes.fromBytes(k)))
                transactionsRoot.remove(k);
        });

        transactionsStore.forEach((k, v) -> {
            if (!txWhiteList.contains(HexBytes.fromBytes(k)))
                transactionsStore.remove(k);
        });

        accountTrie.prune(stateRootWhiteList);
        status.put(PRUNE, header);
    }
}
