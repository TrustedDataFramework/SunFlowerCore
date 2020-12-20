package org.tdf.sunflower.consensus.poa;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.tdf.sunflower.consensus.MinerConfig;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PoAConfig implements MinerConfig {
    private String name;
    private String genesis;
    @JsonProperty("block-interval")
    private int blockInterval;
    @JsonProperty("enable-mining")
    private boolean enableMining;
    @JsonProperty("private-key")
    private String privateKey;
    @JsonProperty("allow-empty-block")
    private boolean allowEmptyBlock;
    @JsonProperty("max-body-size")
    private int maxBodySize;

    // 是否允许未认证的节点同步
    @JsonProperty("allow-unauthorized")
    private boolean allowUnauthorized;
}
