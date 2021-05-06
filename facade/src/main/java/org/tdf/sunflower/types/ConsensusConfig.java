package org.tdf.sunflower.types;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.SneakyThrows;
import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.facade.PropertiesWrapper;
import org.tdf.sunflower.facade.PropertyLike;
import org.tdf.sunflower.util.FileUtils;

import java.io.InputStream;
import java.util.Objects;
import java.util.Properties;

public class ConsensusConfig {
    @Getter
    protected final PropertyLike properties;
    protected final PropertyReader reader;


    public ConsensusConfig(Properties properties){
        this(new PropertiesWrapper(properties));
    }

    public ConsensusConfig(PropertyLike properties) {
        this.properties = properties;
        this.reader = new PropertyReader(properties);
    }

    private String name;
    private Integer blockInterval;
    private Boolean enableMining;
    private HexBytes privateKey;
    private Boolean allowEmptyBlock;
    private HexBytes minerCoinBase;
    private Integer blocksPerEra;
    private Integer maxMiners;


    public String getName() {
        if(name != null)
            return name;
        name = reader.getAsLowerCased("name");
        return name;
    }

    public int getBlockInterval() {
        if(blockInterval != null)
            return blockInterval;
        blockInterval = reader.getAsInt("block-interval");
        return blockInterval;
    }

    public boolean enableMining() {
        if(enableMining != null)
            return enableMining;
        enableMining = reader.getAsBool("enable-mining");
        return enableMining;
    }

    public HexBytes getPrivateKey() {
        if(privateKey != null)
            return privateKey;
        privateKey = reader.getAsPrivate("private-key");
        return privateKey;
    }

    public boolean allowEmptyBlock() {
        if(allowEmptyBlock != null)
            return allowEmptyBlock;
        allowEmptyBlock = reader.getAsBool("allow-empty-block");
        return allowEmptyBlock;
    }

    public HexBytes getMinerCoinBase() {
        if(minerCoinBase != null)
            return minerCoinBase;

        minerCoinBase = reader.getAsAddress("miner-coin-base");
        return minerCoinBase;
    }

    public int getBlocksPerEra() {
        if(blocksPerEra != null)
            return blocksPerEra;
        blocksPerEra = reader.getAsInt("blocks-per-era");
        return blocksPerEra;
    }

    public int getMaxMiners() {
        if(maxMiners != null)
            return maxMiners;
        maxMiners = reader.getAsInt("max-miners");
        return maxMiners;
    }

    @SneakyThrows
    public JsonNode getGenesisJson() {
        ObjectMapper objectMapper = new ObjectMapper()
            .enable(JsonParser.Feature.ALLOW_COMMENTS);

        InputStream in = FileUtils.getInputStream(
            Objects.requireNonNull(properties.getProperty("genesis"))
        );

        return objectMapper.readValue(in, JsonNode.class);
    }
}

