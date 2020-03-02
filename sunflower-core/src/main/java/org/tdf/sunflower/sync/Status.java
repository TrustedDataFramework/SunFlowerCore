package org.tdf.sunflower.sync;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.tdf.common.util.HexBytes;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class Status {
    private long bestBlockHeight;
    private HexBytes bestBlockHash;
    private HexBytes genesisBlockHash;
    private long prunedHeight;
    private HexBytes prunedHash;
}
