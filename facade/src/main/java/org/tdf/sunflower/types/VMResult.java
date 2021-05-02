package org.tdf.sunflower.types;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.tdf.common.types.Uint256;
import org.tdf.common.util.ByteUtil;
import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.state.Address;

import java.util.Collections;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VMResult {
    public static final
    VMResult EMPTY = new VMResult(0, Address.empty(), ByteUtil.EMPTY_BYTE_ARRAY, Collections.emptyList(), Uint256.ZERO);

    long gasUsed;
    HexBytes contractAddress;
    byte[] executionResult;
    List<LogInfo> logs;
    Uint256 fee;
}
