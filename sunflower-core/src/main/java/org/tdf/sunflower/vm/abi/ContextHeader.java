package org.tdf.sunflower.vm.abi;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Value;
import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.types.Header;

@Getter
@AllArgsConstructor
public class ContextHeader {
    private HexBytes parentHash;
    private long createdAt;
    private long height;

    public ContextHeader(Header header){
        this(header.getHashPrev(), header.getCreatedAt(), header.getHeight());
    }
}
