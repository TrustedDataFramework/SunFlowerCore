package org.tdf.sunflower;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.Value;
import org.springframework.core.env.Environment;
import org.tdf.common.types.Uint256;
import org.tdf.sunflower.facade.PropertyLike;
import org.tdf.sunflower.types.PropertyReader;
import org.tdf.sunflower.util.FileUtils;

import java.io.InputStream;
import java.util.Objects;

import static org.tdf.sunflower.types.ConsensusConfig.DEFAULT_BLOCK_GAS_LIMIT;

@RequiredArgsConstructor
public class AppConfig {
    public static AppConfig get() {
        return AppConfig.INSTANCE;
    }

    public static AppConfig INSTANCE = null;

    private Integer trieCacheSize;
    private Integer p2pTransactionCacheSize;
    private Integer p2pProposalCacheSize;
    private Long blockGasLimit;
    private Long maxFrames;
    private Integer maxContractCallDepth;
    private Uint256 vmGasPrice;
    private Boolean trieSecure;

    @Value
    public static class EnvWrapper implements PropertyLike {
        Environment env;

        @Override
        public String getProperty(String key) {
            return env.getProperty(key);
        }
    }

    @Getter
    protected final PropertyLike properties;
    protected final PropertyReader reader;

    public AppConfig(Environment env) {
        this.properties = new EnvWrapper(env);
        this.reader = new PropertyReader(this.properties);
    }

    public int getTrieCacheSize() {
        if (this.trieCacheSize != null)
            return trieCacheSize;
        this.trieCacheSize = reader.getAsInt("sunflower.cache.trie", 32);
        return this.trieCacheSize;
    }

    public int getP2pTransactionCacheSize() {
        if (this.p2pTransactionCacheSize != null)
            return p2pTransactionCacheSize;
        this.p2pTransactionCacheSize = reader.getAsInt("sunflower.cache.p2p.transaction", 128);
        return p2pTransactionCacheSize;
    }

    public int getP2pProposalCacheSize() {
        if (this.p2pProposalCacheSize != null)
            return p2pProposalCacheSize;
        this.p2pProposalCacheSize = reader.getAsInt("sunflower.cache.p2p.proposal", 128);
        return this.p2pProposalCacheSize;
    }

    @SneakyThrows
    public long getBlockGasLimit() {
        if (this.blockGasLimit != null)
            return blockGasLimit;

        ObjectMapper objectMapper = new ObjectMapper()
            .enable(JsonParser.Feature.ALLOW_COMMENTS);

        InputStream in = FileUtils.getInputStream(
            Objects.requireNonNull(properties.getProperty("sunflower.consensus.genesis"))
        );
        JsonNode n = objectMapper.readValue(in, JsonNode.class);
        blockGasLimit = n.get("gasLimit") == null ? DEFAULT_BLOCK_GAS_LIMIT : n.get("gasLimit").asLong();
        return blockGasLimit;
    }

    public long getMaxFrames() {
        if (this.maxFrames != null)
            return this.maxFrames;

        this.maxFrames = reader.getAsLong("sunflower.vm.max-frames");
        return this.maxFrames;
    }

    public int getMaxContractCallDepth() {
        if (this.maxContractCallDepth != null)
            return this.maxContractCallDepth;
        this.maxContractCallDepth = reader.getAsInt("sunflower.vm.max-contract-call-depth");
        return this.maxContractCallDepth;
    }

    public Uint256 getVmGasPrice() {
        if (this.vmGasPrice != null)
            return this.vmGasPrice;
        this.vmGasPrice = reader.getAsU256("sunflower.vm.gas-price", Uint256.ZERO);
        return this.vmGasPrice;
    }

    public boolean isTrieSecure() {
        if (this.trieSecure != null)
            return this.trieSecure;
        this.trieSecure = reader.getAsBool("sunflower.trie.secure");
        return this.trieSecure;
    }
}
