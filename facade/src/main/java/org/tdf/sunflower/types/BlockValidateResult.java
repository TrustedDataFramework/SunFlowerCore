package org.tdf.sunflower.types;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.tdf.common.types.Uint256;
import org.tdf.common.util.HexBytes;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class BlockValidateResult extends ValidateResult {
    private long gas;
    private Uint256 fee;
    private Map<HexBytes, VMResult> results;
    private List<TransactionInfo> infos;

    public BlockValidateResult(boolean success, @NonNull String reason) {
        super(success, reason);
    }

    public static BlockValidateResult fault(String reason) {
        return new BlockValidateResult(false, reason);
    }

    public static BlockValidateResult success() {
        return new BlockValidateResult(true, "");
    }
}
