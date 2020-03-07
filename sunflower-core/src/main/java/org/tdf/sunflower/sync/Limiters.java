package org.tdf.sunflower.sync;

import com.google.common.util.concurrent.RateLimiter;

import java.util.Map;

public class Limiters {
    private RateLimiter status;
    private RateLimiter getBlocks;
    private RateLimiter getAccounts;
    private RateLimiter getAddresses;

    public Limiters(Map<String, Double> config) {
        if (config == null) return;
        if (config.get("status") != null)
            this.status = RateLimiter.create(config.get("status"));
        if (config.get("get-blocks") != null) {
            this.getBlocks = RateLimiter.create(config.get("get-blocks"));
        }
        if(config.get("get-accounts") != null){
            this.getAccounts = RateLimiter.create(config.get("get-accounts"));
        }
        if(config.get("get-addresses") != null){
            this.getAddresses = RateLimiter.create(config.get("get-addresses"));
        }
    }

    public RateLimiter status() {
        return status;
    }

    public RateLimiter getBlocks() {
        return getBlocks;
    }

    public RateLimiter getAccounts() {
        return getAccounts;
    }

    public RateLimiter getAddresses() {
        return getAddresses;
    }
}
