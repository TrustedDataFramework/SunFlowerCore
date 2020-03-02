package org.tdf.sunflower.consensus.poa;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.consensus.poa.config.Genesis;
import org.tdf.sunflower.exception.ConsensusEngineInitException;
import org.tdf.sunflower.facade.ConsensusEngine;
import org.tdf.sunflower.facade.PeerServerListener;
import org.tdf.sunflower.state.Account;
import org.tdf.sunflower.state.AccountTrie;
import org.tdf.sunflower.state.AccountUpdater;
import org.tdf.sunflower.util.FileUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

// poa is a minimal non-trivial consensus engine
@Slf4j
public class PoA extends ConsensusEngine {
    private PoAConfig poAConfig;
    private Genesis genesis;
    private PoAMiner poaMiner;
    private PoAValidator poAValidator;

    public PoA() {
    }


    @Override
    public void init(Properties properties) throws ConsensusEngineInitException {
        JavaPropsMapper mapper = new JavaPropsMapper();
        ObjectMapper objectMapper = new ObjectMapper().enable(JsonParser.Feature.ALLOW_COMMENTS);
        try {
            poAConfig = mapper.readPropertiesAs(properties, PoAConfig.class);
        } catch (Exception e) {
            String schema = "";
            try {
                schema = mapper.writeValueAsProperties(new PoAConfig()).toString();
            } catch (Exception ignored) {
            }
            ;
            throw new ConsensusEngineInitException(
                    "load properties failed :" + properties.toString() + " expecting " + schema
            );
        }
        poaMiner = new PoAMiner();
        Resource resource;
        try {
            resource = FileUtils.getResource(poAConfig.getGenesis());
        } catch (Exception e) {
            throw new ConsensusEngineInitException(e.getMessage());
        }
        try {
            genesis = objectMapper.readValue(resource.getInputStream(), Genesis.class);
        } catch (Exception e) {
            throw new ConsensusEngineInitException("failed to parse genesis");
        }
        poaMiner.setBlockRepository(this.getSunflowerRepository());
        poaMiner.setPoAConfig(poAConfig);
        poaMiner.setGenesis(genesis);
        poaMiner.setTransactionPool(getTransactionPool());

        setMiner(poaMiner);
        setGenesisBlock(genesis.getBlock());

        setPeerServerListener(PeerServerListener.NONE);
        // create state repository

        Map<HexBytes, Account> alloc = new HashMap<>();
        genesis.alloc.forEach((k, v) -> {
            Account a = new Account(HexBytes.fromHex(k), v);
            alloc.put(a.getAddress(), a);
        });

        AccountUpdater updater = new AccountUpdater(alloc);
        AccountTrie trie = new AccountTrie(updater, getDatabaseStoreFactory());
        getGenesisBlock().setStateRoot(trie.getGenesisRoot());
        setAccountTrie(trie);
        poaMiner.setAccountTrie(trie);
        poaMiner.setEventBus(getEventBus());
        poAValidator = new PoAValidator(getAccountTrie());
        setValidator(poAValidator);

        // register dummy account
    }
}
