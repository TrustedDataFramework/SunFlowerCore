package org.tdf.sunflower.sync

import com.google.common.util.concurrent.RateLimiter

class Limiters(config: Map<String, Double>?) {
    var status: RateLimiter? = null
        private set

    var blocks: RateLimiter? = null
        private set

    init {
        config?.let {
            if (it["status"] != null)
                status = RateLimiter.create(it["status"]!!)
            if (it["get-blocks"] != null) {
                blocks = RateLimiter.create(it["get-blocks"]!!)
            }
        }
    }
}