package org.tdf.common;

import java.util.List;

public interface ConfirmedBlocksProvider {
    List<Block> getConfirmed(List<Block> unconfirmed);
}
