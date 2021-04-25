package org.tdf.sunflower.facade;

import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.types.Block;
import org.tdf.sunflower.types.Header;

import java.util.List;
import java.util.Optional;

public interface BlockRepository {
    Block getGenesis();

    void saveGenesis(Block block);

    // TODO: use guava cache
    boolean containsHeader(byte[] hash);

    Header getBestHeader();

    Block getBestBlock();

    // TODO: use guava cache
    Optional<Header> getHeader(byte[] hash);

    // TODO: use guava cache
    Optional<Block> getBlock(byte[] hash);

    List<Header> getHeadersBetween(long startHeight, long stopHeight);

    List<Block> getBlocksBetween(long startHeight, long stopHeight);

    List<Header> getHeadersBetween(long startHeight, long stopHeight, int limit);

    List<Header> getHeadersBetween(long startHeight, long stopHeight, int limit, boolean descend);

    List<Block> getBlocksBetween(long startHeight, long stopHeight, int limit);

    List<Block> getBlocksBetween(long startHeight, long stopHeight, int limit, boolean descend);

    List<Header> getHeadersByHeight(long height);

    List<Block> getBlocksByHeight(long height);

    Optional<Block> getCanonicalBlock(long height);

    Optional<Header> getCanonicalHeader(long height);

    void writeBlock(Block block);

    // delete all header and transactions until this height(inclusive), exclude this block
//    void prune(byte[] hash);

    // pruned header height
//    long getPrunedHeight();

    // hash of pruned header
//    HexBytes getPrunedHash();
}
