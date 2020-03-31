package org.tdf.sunflower.consensus.vrf.contract;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class WithdrawParams {
    @JsonProperty("withdrawAmount")
    private long withdrawAmount;
}
