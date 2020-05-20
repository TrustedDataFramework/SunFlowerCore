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
import org.tdf.sunflower.state.Authentication;
import org.tdf.sunflower.types.Block;
import org.tdf.sunflower.util.FileUtils;
import org.tdf.sunflower.util.MappingUtil;

import java.util.*;
import java.util.stream.Collectors;

// poa is a minimal non-trivial consensus engine
@Slf4j(topic = "poa")
public class PoA extends AbstractConsensusEngine {
    private PoAConfig poAConfig;
    private Genesis genesis;
    private PoAMiner poaMiner;
    private PoAValidator poAValidator;
    private AccountTrie accountTrie;

    public PoA() {
    }

    @Override
    public Optional<Set<HexBytes>> getApprovedNodes() {
        if (!poAConfig.isAuth())
            return Optional.empty();

        Block best = getSunflowerRepository().getBestBlock();
        return Optional.of(new HashSet<>(Authentication.getNodes(accountTrie, best.getStateRoot().getBytes())));
    }

    @Override
    public void init(Properties properties) throws ConsensusEngineInitException {
        ObjectMapper objectMapper = new ObjectMapper().enable(JsonParser.Feature.ALLOW_COMMENTS);
        poAConfig = MappingUtil.propertiesToPojo(properties, PoAConfig.class);
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
                poAConfig.isAuth() ? Collections.singletonList(new Authentication(
                        genesis.miners == null ? Collections.emptyList() :
                                genesis.miners.stream().map(Genesis.MinerInfo::getAddress).collect(Collectors.toSet())
                )) : Collections.emptyList(),
                Collections.emptyList()
        );
        AccountTrie trie = new AccountTrie(
                updater, getDatabaseStoreFactory(),
                getContractCodeStore(), getContractStorageTrie()
        );
        this.accountTrie = trie;
        getGenesisBlock().setStateRoot(trie.getGenesisRoot());
        setAccountTrie(trie);

        poaMiner = new PoAMiner(getAccountTrie(), getEventBus(), poAConfig);
        poaMiner.setBlockRepository(this.getSunflowerRepository());
        poaMiner.setPoAConfig(poAConfig);
        poaMiner.setGenesis(genesis);
        poaMiner.setTransactionPool(getTransactionPool());

        setMiner(poaMiner);

        poAValidator = new PoAValidator(getAccountTrie());
        setValidator(poAValidator);

        // register dummy account
    }
}
