package org.tdf.sunflower.consensus.vrf.contract;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class DepositParams {
    @JsonProperty("depositAmount")
    private long depositAmount;
}
