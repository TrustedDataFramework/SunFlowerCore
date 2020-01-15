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

    boolean containsBlock(byte[] hash);

    Header getBestHeader();

    Block getBestBlock();

    Optional<Header> getHeader(byte[] hash);

    Optional<Block> getBlock(byte[] hash);

    List<Header> getHeaders(long startHeight, int limit);

    List<Block> getBlocks(long startHeight, int limit);

    List<Header> getHeadersBetween(long startHeight, long stopHeight);

    List<Block> getBlocksBetween(long startHeight, long stopHeight);

    List<Header> getHeadersBetween(long startHeight, long stopHeight, int limit);

    List<Header> getHeadersBetweenDescend(long startHeight, long stopHeight, int limit);

    List<Block> getBlocksBetween(long startHeight, long stopHeight, int limit);

    List<Block> getBlocksBetweenDescend(long startHeight, long stopHeight, int limit);

    Optional<Header> getHeaderByHeight(long height);

    Optional<Block> getBlockByHeight(long height);

    List<Header> getAncestorHeaders(byte[] hash, int limit);

    List<Block> getAncestorBlocks(byte[] hash, int limit);

    void writeBlock(Block block);
}
