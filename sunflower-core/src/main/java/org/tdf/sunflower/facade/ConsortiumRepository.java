package org.tdf.sunflower.facade;

import org.tdf.sunflower.exception.GenesisConflictsException;
import org.tdf.sunflower.exception.WriteGenesisFailedException;
import org.tdf.sunflower.types.Block;
import org.tdf.sunflower.types.Header;
import org.tdf.sunflower.types.Transaction;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public interface ConsortiumRepository extends BlockRepository, TransactionRepository {
    Block getLastConfirmed();

    List<Block> getUnconfirmed();

    void setProvider(ConfirmedBlocksProvider provider);

    void addListeners(ConsortiumRepositoryListener... listeners);

    ConsortiumRepository NONE = new ConsortiumRepository() {
        @Override
        public Block getLastConfirmed() {
            return new Block();
        }

        @Override
        public List<Block> getUnconfirmed() {
            return Collections.emptyList();
        }

        @Override
        public void setProvider(ConfirmedBlocksProvider provider) {

        }

        @Override
        public void addListeners(ConsortiumRepositoryListener... listeners) {

        }

        @Override
        public Block getGenesis() {
            return new Block();
        }

        @Override
        public void saveGenesisBlock(Block block) throws GenesisConflictsException, WriteGenesisFailedException {

        }

        @Override
        public boolean hasBlock(byte[] hash) {
            return false;
        }

        @Override
        public Header getBestHeader() {
            return new Header();
        }

        @Override
        public Block getBestBlock() {
            return new Block();
        }

        @Override
        public Optional<Header> getHeader(byte[] hash) {
            return Optional.empty();
        }

        @Override
        public Optional<Block> getBlock(byte[] hash) {
            return Optional.empty();
        }

        @Override
        public List<Header> getHeaders(long startHeight, int limit) {
            return Collections.emptyList();
        }

        @Override
        public List<Block> getBlocks(long startHeight, int limit) {
            return Collections.emptyList();
        }

        @Override
        public List<Header> getHeadersBetween(long startHeight, long stopHeight) {
            return Collections.emptyList();
        }

        @Override
        public List<Block> getBlocksBetween(long startHeight, long stopHeight) {
            return Collections.emptyList();
        }

        @Override
        public List<Header> getHeadersBetween(long startHeight, long stopHeight, int limit) {
            return Collections.emptyList();
        }

        @Override
        public List<Header> getHeadersBetweenDescend(long startHeight, long stopHeight, int limit) {
            return Collections.emptyList();
        }

        @Override
        public List<Block> getBlocksBetween(long startHeight, long stopHeight, int limit) {
            return Collections.emptyList();
        }

        @Override
        public List<Block> getBlocksBetweenDescend(long startHeight, long stopHeight, int limit) {
            return Collections.emptyList();
        }

        @Override
        public Optional<Header> getHeaderByHeight(long height) {
            return Optional.empty();
        }

        @Override
        public Optional<Block> getBlockByHeight(long height) {
            return Optional.empty();
        }

        @Override
        public Optional<Header> getAncestorHeader(byte[] hash, long ancestorHeight) {
            return Optional.empty();
        }

        @Override
        public Optional<Block> getAncestorBlock(byte[] hash, long ancestorHeight) {
            return Optional.empty();
        }

        @Override
        public List<Header> getAncestorHeaders(byte[] hash, int limit) {
            return Collections.emptyList();
        }

        @Override
        public List<Block> getAncestorBlocks(byte[] hash, int limit) {
            return Collections.emptyList();
        }

        @Override
        public boolean writeBlock(Block block) {
            return false;
        }

        @Override
        public boolean hasTransaction(byte[] hash) {
            return false;
        }

        @Override
        public boolean hasPayload(byte[] payload) {
            return false;
        }

        @Override
        public Optional<Transaction> getTransactionByHash(byte[] hash) {
            return Optional.empty();
        }

        @Override
        public List<Transaction> getTransactionsByFrom(byte[] from, int page, int size) {
            return Collections.emptyList();
        }

        @Override
        public List<Transaction> getTransactionsByFromAndType(byte[] from, int type, int page, int size) {
            return Collections.emptyList();
        }

        @Override
        public List<Transaction> getTransactionsByTo(byte[] to, int page, int size) {
            return Collections.emptyList();
        }

        @Override
        public List<Transaction> getTransactionsByToAndType(byte[] to, int type, int page, int size) {
            return Collections.emptyList();
        }

        @Override
        public List<Transaction> getTransactionsByFromAndTo(byte[] from, byte[] to, int page, int size) {
            return Collections.emptyList();
        }

        @Override
        public List<Transaction> getTransactionsByFromAndToAndType(byte[] from, byte[] to, int type, int page, int size) {
            return Collections.emptyList();
        }

        @Override
        public List<Transaction> getTransactionsByPayload(byte[] payload, int page, int size) {
            return Collections.emptyList();
        }

        @Override
        public List<Transaction> getTransactionsByPayloadAndType(byte[] payload, int type, int page, int size) {
            return Collections.emptyList();
        }

        @Override
        public List<Transaction> getTransactionsByBlockHash(byte[] blockHash, int page, int size) {
            return Collections.emptyList();
        }

        @Override
        public List<Transaction> getTransactionsByBlockHeight(long height, int page, int size) {
            return Collections.emptyList();
        }
    };
}
