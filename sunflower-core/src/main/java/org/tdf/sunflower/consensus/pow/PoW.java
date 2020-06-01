package org.tdf.sunflower.consensus.pow;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.tdf.common.util.BigEndian;
import org.tdf.common.util.HexBytes;
import org.tdf.rlp.RLPCodec;
import org.tdf.sunflower.Start;
import org.tdf.sunflower.facade.AbstractConsensusEngine;
import org.tdf.sunflower.facade.PeerServerListener;
import org.tdf.sunflower.state.Account;
import org.tdf.sunflower.state.Bios;
import org.tdf.sunflower.types.Block;
import org.tdf.sunflower.types.CryptoContext;
import org.tdf.sunflower.util.FileUtils;
import org.tdf.sunflower.util.MappingUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import static org.tdf.sunflower.consensus.pow.PoWBios.N_BITS_KEY;

@Slf4j(topic = "pow")
public class PoW extends AbstractConsensusEngine {
    public static final int BLOCK_VERSION = BigEndian.decodeInt32(new byte[]{0, 'p', 'o', 'w'});
    public static final int TRANSACTION_VERSION = BigEndian.decodeInt32(new byte[]{0, 'p', 'o', 'w'});
    private Genesis genesis;
    private PoWConfig config;

    public PoW() {

    }

    public static byte[] getPoWHash(Block block) {
        byte[] d = CryptoContext.hash(RLPCodec.encode(block.getHeader()));
        return CryptoContext.hash(d);
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
    public List<Account> getGenesisStates() {
        if (genesis.getAlloc() == null)
            return Collections.emptyList();

        List<Account> accounts = new ArrayList<>();

        if (genesis.getAlloc() != null) {
            genesis.getAlloc().forEach((k, v) -> {
                Account a = new Account(HexBytes.fromHex(k), v);
                accounts.add(a);
            });
        }

        return accounts;
    }

    @Override
    public List<Bios> getBios() {
        PoWBios bios = new PoWBios(genesis.getNbits().getBytes(), config);
        return Collections.singletonList(bios);
    }

    @Override
    @SneakyThrows
    public void init(Properties properties) {
        ObjectMapper objectMapper = new ObjectMapper().enable(JsonParser.Feature.ALLOW_COMMENTS);
        config = MappingUtil.propertiesToPojo(properties, PoWConfig.class);
        Resource resource = FileUtils.getResource(config.getGenesis());
        genesis = objectMapper.readValue(resource.getInputStream(), Genesis.class);

        setGenesisBlock(genesis.get());
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
