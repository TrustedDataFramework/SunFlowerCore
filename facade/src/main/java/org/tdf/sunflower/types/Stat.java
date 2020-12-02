package org.tdf.sunflower.types;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.tdf.common.types.Uint256;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Stat {
    // cpu usage
    private double cpu;

    // memory used (bytes)
    private long memoryUsed;

    // total memory (bytes)
    private long totalMemory;

    // the best block height
    private long height;

    // average block internal (last 10 block)
    private double averageBlockInterval;

    // average transaction fee (last
    private Uint256 averageGasPrice;

    private long transactionPoolSize;

    // if current consensus is pow, returns current difficulty
    private String currentDifficulty;

    // if is mining, the coin base exists in last 10 blocks
    private boolean mining;

    // blocks per day =
    private long blocksPerDay;

    // p2p 网络地址
    private String p2pAddress;

    // 创世区块文件
    private JsonNode genesis;

    // 哈希算法
    private String hash;

    // 签名算法
    private String ec;

    // 对称加密算法
    private String ae;

    // 出块的速度
    private int blockInterval;

    // pos 矿工数量
    private int maxMiners;

    // pow 每个纪元区块数量
    private int blocksPerEra;

    // poa 是否允许未认证节点同步区块
    private boolean allowUnauthorized;

    // tds version
    @Builder.Default
    private String version = "v1.0.0";

    // 共识机制
    private String consensus;
}
