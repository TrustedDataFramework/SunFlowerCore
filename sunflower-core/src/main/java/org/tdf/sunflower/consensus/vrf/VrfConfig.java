package org.tdf.sunflower.consensus.vrf;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VrfConfig {
    private String name;
    private String genesis;
    @JsonProperty("block-interval")
    private int blockInterval;
    @JsonProperty("enable-mining")
    private boolean enableMining;
    @JsonProperty("miner-coin-base")
    private String minerCoinBase;
    @JsonProperty("vrf-data-dir")
    private String vrfDataDir;
    @JsonProperty("allow-empty-block")
    private boolean allowEmptyBlock;
    @JsonProperty("max-body-size")
    private int maxBodySize;
}
