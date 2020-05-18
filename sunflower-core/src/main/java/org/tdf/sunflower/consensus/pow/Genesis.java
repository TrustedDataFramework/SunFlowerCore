package org.tdf.sunflower.consensus.pow;

import org.tdf.common.util.HexBytes;

import java.util.Map;

public class Genesis {
    public HexBytes parentHash;

    public long timestamp;

    private Map<HexBytes, Long> alloc;


}
