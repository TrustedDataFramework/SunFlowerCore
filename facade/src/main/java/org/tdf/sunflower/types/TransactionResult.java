package org.tdf.sunflower.types;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.tdf.common.types.Uint256;
import org.tdf.common.util.ByteUtil;
import org.tdf.rlp.RLPList;

import java.util.Collections;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransactionResult {
    public static final TransactionResult EMPTY = new TransactionResult(0, ByteUtil.EMPTY_BYTE_ARRAY, Collections.emptyList(), Uint256.ZERO);
    long gasUsed;
    byte[] returns;
    List<Event> events;
    Uint256 fee;
}
