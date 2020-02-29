package org.tdf.sunflower.facade;

import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.exception.GenesisConflictsException;
import org.tdf.sunflower.exception.WriteGenesisFailedException;
import org.tdf.sunflower.state.Account;
import org.tdf.sunflower.state.StateTrie;
import org.tdf.sunflower.types.Block;
import org.tdf.sunflower.types.Header;
import org.tdf.sunflower.types.Transaction;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public interface SunflowerRepository extends BlockRepository, TransactionRepository {


    void setProvider(ConfirmedBlocksProvider provider);
    void setAccountTrie(StateTrie<HexBytes, Account> accountTrie);

    SunflowerRepository NONE = new SunflowerRepository() {

        @Override
        public void setProvider(ConfirmedBlocksProvider provider) {

        }

        @Override
        public void setAccountTrie(StateTrie<HexBytes, Account> accountTrie) {

        }

        @Override
        public Block getGenesis() {
            return new Block();
        }

        @Override
        public void saveGenesis(Block block) throws GenesisConflictsException, WriteGenesisFailedException {

        }

        @Override
        public boolean containsHeader(byte[] hash) {
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
        public List<Header> getHeadersBetween(long startHeight, long stopHeight, int limit, boolean descend) {
            return null;
        }


        @Override
        public List<Block> getBlocksBetween(long startHeight, long stopHeight, int limit) {
            return Collections.emptyList();
        }

        @Override
        public List<Block> getBlocksBetween(long startHeight, long stopHeight, int limit, boolean descend) {
            return null;
        }


        @Override
        public List<Header> getHeadersByHeight(long height) {
            return null;
        }

        @Override
        public List<Block> getBlocksByHeight(long height) {
            return null;
        }

        @Override
        public void writeBlock(Block block) {

        }

        @Override
        public void writeHeader(Header header) {

        }

        @Override
        public void prune(byte[] hash) {

        }

        @Override
        public boolean containsTransaction(byte[] hash) {
            return false;
        }

        @Override
        public Optional<Transaction> getTransactionByHash(byte[] hash) {
            return Optional.empty();
        }

        @Override
        public List<Transaction> getTransactionsByBlockHash(byte[] blockHash) {
            return Collections.emptyList();
        }
    };
}
