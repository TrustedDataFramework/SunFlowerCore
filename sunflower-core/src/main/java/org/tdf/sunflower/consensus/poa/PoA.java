package org.tdf.sunflower.consensus.poa;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.consensus.poa.config.Genesis;
import org.tdf.sunflower.exception.ConsensusEngineInitException;
import org.tdf.sunflower.facade.AbstractConsensusEngine;
import org.tdf.sunflower.facade.PeerServerListener;
import org.tdf.sunflower.state.Account;
import org.tdf.sunflower.state.AccountTrie;
import org.tdf.sunflower.state.AccountUpdater;
import org.tdf.sunflower.util.FileUtils;
import org.tdf.sunflower.util.MappingUtil;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

// poa is a minimal non-trivial consensus engine
@Slf4j(topic = "poa")
public class PoA extends AbstractConsensusEngine {
    private PoAConfig poAConfig;
    private Genesis genesis;
    private PoAMiner poaMiner;
    private PoAValidator poAValidator;

    public PoA() {
    }


    @Override
    public void init(Properties properties) throws ConsensusEngineInitException {
        ObjectMapper objectMapper = new ObjectMapper().enable(JsonParser.Feature.ALLOW_COMMENTS);
        poAConfig = MappingUtil.propertiesToPojo(properties, PoAConfig.class);
        poaMiner = new PoAMiner(poAConfig);
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

        if (genesis.alloc != null) {
            genesis.alloc.forEach((k, v) -> {
                Account a = new Account(HexBytes.fromHex(k), v);
                alloc.put(a.getAddress(), a);
            });
        }

        AccountUpdater updater = new AccountUpdater(
                alloc, getContractCodeStore(),
                getContractStorageTrie(),
                Collections.emptyList(),
                Collections.emptyList()
        );
        AccountTrie trie = new AccountTrie(
                updater, getDatabaseStoreFactory(),
                getContractCodeStore(), getContractStorageTrie()
        );
        getGenesisBlock().setStateRoot(trie.getGenesisRoot());
        setAccountTrie(trie);
        poaMiner.setAccountTrie(trie);
        poaMiner.setEventBus(getEventBus());
        poAValidator = new PoAValidator(getAccountTrie());
        setValidator(poAValidator);

        // register dummy account
    }
}
