package org.tdf.sunflower;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.core.env.Environment;
import org.tdf.common.types.Uint256;
import org.tdf.sunflower.facade.PropertyLike;
import org.tdf.sunflower.types.PropertyReader;

@RequiredArgsConstructor
public class AppConfig {
    public static AppConfig get() {
        return AppConfig.INSTANCE;
    }

    public static AppConfig INSTANCE = null;

    private Integer trieCacheSize;
    private Integer p2pTransactionCacheSize;
    private Integer p2pProposalCacheSize;
    private Long vmStepLimit;
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

    public long getStepLimit() {
        if (this.vmStepLimit != null)
            return vmStepLimit;
        this.vmStepLimit = reader.getAsLong("sunflower.vm.step-limit");
        return this.vmStepLimit;
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
