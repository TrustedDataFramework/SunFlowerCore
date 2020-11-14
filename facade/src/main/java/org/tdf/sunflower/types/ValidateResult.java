package org.tdf.sunflower.types;

import lombok.Getter;
import lombok.NonNull;

@Getter
public class ValidateResult {
    private static final ValidateResult SUCCESS = new ValidateResult(true, "");
    private boolean success;
    @NonNull
    private String reason;


    public ValidateResult(boolean success, @NonNull String reason) {
        this.success = success;
        this.reason = reason;
    }

    public static ValidateResult success() {
        return SUCCESS;
    }

    public static ValidateResult fault(String reason) {
        return new ValidateResult(false, reason);
    }
}
