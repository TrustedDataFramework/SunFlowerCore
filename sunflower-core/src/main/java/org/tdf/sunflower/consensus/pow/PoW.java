package org.tdf.sunflower.consensus.pow;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.springframework.core.io.Resource;
import org.tdf.common.util.BigEndian;
import org.tdf.rlp.RLPCodec;
import org.tdf.sunflower.crypto.CryptoContext;
import org.tdf.sunflower.facade.AbstractConsensusEngine;
import org.tdf.sunflower.state.Account;
import org.tdf.sunflower.types.Block;
import org.tdf.sunflower.util.FileUtils;
import org.tdf.sunflower.util.MappingUtil;

import java.util.Properties;

import static org.tdf.sunflower.consensus.pow.PoWBios.N_BITS_KEY;

public class PoW extends AbstractConsensusEngine {
    public static final int BLOCK_VERSION = BigEndian.decodeInt32(new byte[]{0, 'p', 'o', 'w'});
    public static final int TRANSACTION_VERSION = BigEndian.decodeInt32(new byte[]{0, 'p', 'o', 'w'});

    public static byte[] getPoWHash(Block block) {
        byte[] d = CryptoContext.digest(RLPCodec.encode(block.getHeader()));
        return CryptoContext.digest(d);
    }

    public byte[] getNBits(
            byte[] stateRoot) {
        Account a = getAccountTrie().get(stateRoot, PoWBios.ADDRESS).get();
        return getContractStorageTrie().revert(a.getStorageRoot())
                .get(N_BITS_KEY).get();
    }

    public static int compare(byte[] x, byte[] y) {
        if (x.length != y.length)
            throw new RuntimeException("x y length");
        for (int i = 0; i < x.length; i++) {
            int a = Byte.toUnsignedInt(x[i]);
            int b = Byte.toUnsignedInt(y[i]);
            int ret = Integer.compare(a, b);
            if (ret != 0) return ret;
        }
        return 0;
    }

    @Override
    @SneakyThrows
    public void init(Properties properties) {
        ObjectMapper objectMapper = new ObjectMapper().enable(JsonParser.Feature.ALLOW_COMMENTS);
        PoWConfig config = MappingUtil.propertiesToPojo(properties, PoWConfig.class);
        Resource resource = FileUtils.getResource(config.getGenesis());
        Genesis genesis = objectMapper.readValue(resource.getInputStream(), Genesis.class);
        setMiner(new PoWMiner(config, getTransactionPool()));

        setGenesisBlock(genesis.get());
    }

}
