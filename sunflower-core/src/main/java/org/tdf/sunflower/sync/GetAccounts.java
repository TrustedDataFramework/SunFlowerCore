package org.tdf.sunflower.sync;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.tdf.common.util.HexBytes;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GetAccounts {
    private HexBytes stateRoot;
    private List<HexBytes> addresses;
}
