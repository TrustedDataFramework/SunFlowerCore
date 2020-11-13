package org.tdf.sunflower.types;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.tdf.common.types.Uint256;
import org.tdf.common.util.HexBytes;

import java.util.Map;

public class BlockValidateResult extends ValidateResult{

    public static BlockValidateResult fault(String reason) {
        return new BlockValidateResult(false, reason);
    }

    public static BlockValidateResult success() {
        return new BlockValidateResult(true, "");
    }


    public BlockValidateResult(boolean success, @NonNull String reason) {
        super(success, reason);
    }

    @Getter
    @Setter
    private long gas;

    @Getter
    @Setter
    private Uint256 fee;

    @Getter
    @Setter
    private Map<HexBytes, TransactionResult> results;
}
