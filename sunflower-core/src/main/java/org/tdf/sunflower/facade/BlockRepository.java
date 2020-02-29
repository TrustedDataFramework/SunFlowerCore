package org.tdf.sunflower.facade;

import org.tdf.sunflower.exception.GenesisConflictsException;
import org.tdf.sunflower.exception.WriteGenesisFailedException;
import org.tdf.sunflower.types.Block;
import org.tdf.sunflower.types.Header;

import java.util.List;
import java.util.Optional;

public interface BlockRepository{
    Block getGenesis();

    void saveGenesis(Block block) throws GenesisConflictsException, WriteGenesisFailedException;

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

    void writeBlock(Block block);

    void writeHeader(Header header);

    // delete all header and transactions until this height
    void prune(byte[] hash);
}
