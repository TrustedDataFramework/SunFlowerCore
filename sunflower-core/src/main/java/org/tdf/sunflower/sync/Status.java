package org.tdf.sunflower.sync;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.tdf.common.util.HexBytes;


@AllArgsConstructor
@NoArgsConstructor
@Getter
public class Status {
    private long bestBlockHeight;
    private HexBytes bestBlockHash;
    private HexBytes genesisBlockHash;
    private long prunedHeight;
    private HexBytes prunedHash;
}
