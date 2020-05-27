package org.tdf.sunflower.consensus.pos;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.consensus.MinerConfig;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PoSConfig implements MinerConfig {
    private String name;
    private String genesis;
    @JsonProperty("block-interval")
    private int blockInterval;
    @JsonProperty("enable-mining")
    private boolean enableMining;
    @JsonProperty("miner-coin-base")
    private HexBytes minerCoinBase;
    @JsonProperty("allow-empty-block")
    private boolean allowEmptyBlock;
    @JsonProperty("max-body-size")
    private int maxBodySize;
    @JsonProperty("max-miners")
    private int maxMiners;
}
