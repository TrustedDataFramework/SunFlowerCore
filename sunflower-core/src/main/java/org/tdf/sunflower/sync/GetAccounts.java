package org.tdf.sunflower.sync;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.tdf.common.util.HexBytes;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class GetAccounts {
    private HexBytes stateRoot;
    private int maxAccounts;
}
