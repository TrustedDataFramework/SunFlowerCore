package org.tdf.sunflower.consensus.pow;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.tdf.common.util.BigEndian;
import org.tdf.sunflower.exception.ConsensusEngineInitException;
import org.tdf.sunflower.facade.AbstractConsensusEngine;
import org.tdf.sunflower.util.MappingUtil;

import java.util.Properties;

public class PoW extends AbstractConsensusEngine {
    public static final int BLOCK_VERSION = BigEndian.decodeInt32(new byte[]{0, 'p', 'o', 'w'});
    public static final int TRANSACTION_VERSION = BigEndian.decodeInt32(new byte[]{0, 'p', 'o', 'w'});

    @Override
    public void init(Properties properties) throws ConsensusEngineInitException {
        PoWConfig config = MappingUtil.propertiesToPojo(properties, PoWConfig.class);

        setMiner(new PoWMiner(config, getTransactionPool()));
    }

}
