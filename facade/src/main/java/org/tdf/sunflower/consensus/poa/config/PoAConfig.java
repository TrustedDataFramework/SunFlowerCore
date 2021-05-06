package org.tdf.sunflower.consensus.poa.config;

import org.tdf.common.crypto.ECKey;
import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.facade.PropertyLike;
import org.tdf.sunflower.types.ConsensusConfig;

public class PoAConfig extends ConsensusConfig {
    public static PoAConfig from(ConsensusConfig c) {
        return new PoAConfig(c.getProperties());
    }

    public PoAConfig(PropertyLike properties) {
        super(properties);
    }

    private Integer threadId;
    private HexBytes farmBaseAdmin;
    private String gatewayNode;
    private HexBytes minerAddress;

    public int getThreadId() {
        if(threadId != null)
            return threadId;
        threadId = reader.getAsInt("thread-id", 0);
        return threadId;
    }

    public HexBytes getFarmBaseAdmin() {
        if(farmBaseAdmin != null)
            return farmBaseAdmin;
        farmBaseAdmin = reader.getAsAddress("farm-base-admin");
        return farmBaseAdmin;
    }

    public String getGatewayNode() {
        if(gatewayNode != null)
            return gatewayNode;
        gatewayNode = reader.getAsNonNull("gateway-node");
        return gatewayNode;
    }

    public HexBytes getMinerCoinBase() {
        if(minerAddress != null)
            return minerAddress;
        ECKey key = ECKey.fromPrivate(getPrivateKey().getBytes());
        minerAddress = HexBytes.fromBytes(key.getAddress());
        return minerAddress;
    }

    public boolean isControlled() {
        return getThreadId() != 0;
    }
}
