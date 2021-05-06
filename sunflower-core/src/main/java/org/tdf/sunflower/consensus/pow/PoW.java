package org.tdf.sunflower.consensus.pow;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.tdf.common.types.Uint256;
import org.tdf.common.util.HexBytes;
import org.tdf.rlp.RLPCodec;
import org.tdf.sunflower.Start;
import org.tdf.sunflower.facade.AbstractConsensusEngine;
import org.tdf.sunflower.facade.PeerServerListener;
import org.tdf.sunflower.state.Account;
import org.tdf.sunflower.state.BuiltinContract;
import org.tdf.sunflower.types.Block;
import org.tdf.sunflower.types.ConsensusConfig;
import org.tdf.sunflower.types.CryptoContext;

import java.util.Collections;
import java.util.List;

import static org.tdf.sunflower.consensus.pow.PoWBios.N_BITS_KEY;

@Slf4j(topic = "pow")
public class PoW extends AbstractConsensusEngine {
    private Genesis genesis;
    private ConsensusConfig config;

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

    public Uint256 getNBits(
        HexBytes stateRoot) {
        Account a = getAccountTrie().get(stateRoot, PoWBios.ADDRESS);
        HexBytes bytes = getContractStorageTrie().revert(a.getStorageRoot())
            .get(N_BITS_KEY);
        return Uint256.of(bytes.getBytes());
    }

    @Override
    public List<Account> getAlloc() {
        return genesis.getAlloc();
    }

    @Override
    public List<BuiltinContract> getBios() {
        PoWBios bios = new PoWBios(genesis.getNbits(), config);
        return Collections.singletonList(bios);
    }

    @Override
    @SneakyThrows
    public void init(ConsensusConfig config) {
        this.config = config;
        genesis = new Genesis(config.getGenesisJson());

        setGenesisBlock(genesis.getBlock());
        initStateTrie();

        setValidator(new PoWValidator(this));
        setPeerServerListener(PeerServerListener.NONE);

        setMiner(new PoWMiner(config, getTransactionPool(), this));
        log.info("genesis = {}", Start.MAPPER.writeValueAsString(getGenesisBlock()));
    }

    @Override
    public String getName() {
        return "pow";
    }
}
