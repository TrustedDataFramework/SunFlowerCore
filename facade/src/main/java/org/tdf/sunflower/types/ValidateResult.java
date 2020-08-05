package org.tdf.sunflower.types;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

@Getter
public class ValidateResult {
    private static final ValidateResult SUCCESS = new ValidateResult(true, "");
    private boolean success;
    @NonNull
    private String reason;

    @Setter
    private Object ctx;

    public static ValidateResult success() {
        return SUCCESS;
    }

    public static ValidateResult fault(String reason) {
        return new ValidateResult(false, reason);
    }

    public ValidateResult(boolean success, @NonNull String reason) {
        this.success = success;
        this.reason = reason;
    }
}
