package org.tdf.sunflower.consensus.pow;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.tdf.rlp.RLPCodec;
import org.tdf.sunflower.Start;
import org.tdf.sunflower.facade.AbstractConsensusEngine;
import org.tdf.sunflower.state.Account;
import org.tdf.sunflower.state.BuiltinContract;
import org.tdf.sunflower.types.Block;
import org.tdf.sunflower.types.ConsensusConfig;
import org.tdf.sunflower.types.CryptoContext;

import java.util.Collections;
import java.util.List;


@Slf4j(topic = "pow")
public class PoW extends AbstractConsensusEngine {
    private Genesis genesis;
    private ConsensusConfig config;
    PoWBios bios;

    public PoW() {

    }

    public static byte[] getPoWHash(Block block) {
        byte[] d = CryptoContext.hash(RLPCodec.encode(block.getHeader()));
        return CryptoContext.hash(d);
    }

    public static int compare(byte[] x, byte[] y) {
        if (x.length != y.length)
            throw new RuntimeException("compare failed for x and y, length not equal");
        for (int i = 0; i < x.length; i++) {
            int a = Byte.toUnsignedInt(x[i]);
            int b = Byte.toUnsignedInt(y[i]);
            int ret = Integer.compare(a, b);
            if (ret != 0) return ret;
        }
        return 0;
    }

    @Override
    public List<Account> getAlloc() {
        return genesis.getAlloc();
    }

    @Override
    public List<BuiltinContract> getBios() {
        return Collections.singletonList(bios);
    }

    @Override
    @SneakyThrows
    public void init(ConsensusConfig config) {
        this.config = config;
        genesis = new Genesis(config.getGenesisJson());
        setGenesisBlock(genesis.getBlock());

        setValidator(new PoWValidator(this));

        setMiner(new PoWMiner(config, getTransactionPool(), this));
        log.info("genesis = {}", Start.MAPPER.writeValueAsString(getGenesisBlock()));
    }

    @Override
    public String getName() {
        return "pow";
    }
}
