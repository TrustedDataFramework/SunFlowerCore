package org.tdf.sunflower.consensus.poa;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.tdf.sunflower.Start;
import org.tdf.sunflower.consensus.poa.config.Genesis;
import org.tdf.sunflower.exception.ConsensusEngineInitException;
import org.tdf.sunflower.facade.ConsensusEngine;
import org.tdf.sunflower.facade.MinerListener;
import org.tdf.sunflower.facade.PeerServerListener;
import org.tdf.sunflower.net.Context;
import org.tdf.sunflower.net.Peer;
import org.tdf.sunflower.net.PeerServer;
import org.tdf.sunflower.state.AccountTrie;
import org.tdf.sunflower.state.AccountUpdater;
import org.tdf.sunflower.types.Block;
import org.tdf.sunflower.util.FileUtils;

import java.util.Collections;
import java.util.Properties;

// poa is a minimal non-trivial consensus engine
@Slf4j
public class PoA extends ConsensusEngine implements PeerServerListener {
    private PoAConfig poAConfig;
    private Genesis genesis;
    private PoAMiner poaMiner;
    private PoAValidator poAValidator;

    public PoA() {
    }

    private PeerServer peerServer;

    @Override
    public Block getGenesisBlock() {
        Block genesisBlock = super.getGenesisBlock();
        if (genesisBlock != null) return genesisBlock;
        genesisBlock = genesis.getBlock();
        setGenesisBlock(genesisBlock);
        return genesisBlock;
    }

    @Override
    public void init(Properties properties) throws ConsensusEngineInitException {
        JavaPropsMapper mapper = new JavaPropsMapper();
        ObjectMapper objectMapper = new ObjectMapper().enable(JsonParser.Feature.ALLOW_COMMENTS);
        try{
            poAConfig = mapper.readPropertiesAs(properties, PoAConfig.class);
        }catch (Exception e){
            String schema = "";
            try{
                schema = mapper.writeValueAsProperties(new PoAConfig()).toString();
            }catch (Exception ignored){};
            throw new ConsensusEngineInitException(
                    "load properties failed :" + properties.toString() + " expecting " + schema
            );
        }
        poaMiner = new PoAMiner();
        Resource resource;
        try{
            resource = FileUtils.getResource(poAConfig.getGenesis());
        }catch (Exception e){
            throw new ConsensusEngineInitException(e.getMessage());
        }
        try{
            genesis = objectMapper.readValue(resource.getInputStream(), Genesis.class);
        }catch (Exception e){
            throw new ConsensusEngineInitException("failed to parse genesis");
        }
        poaMiner.setBlockRepository(this.getSunflowerRepository());
        poaMiner.setPoAConfig(poAConfig);
        poaMiner.setGenesis(genesis);
        poaMiner.setTransactionPool(getTransactionPool());

        setMiner(poaMiner);
        setGenesisBlock(genesis.getBlock());

        setPeerServerListener(this);
        // create state repository
        AccountUpdater updater = new AccountUpdater(Collections.emptyMap());
        AccountTrie trie = new AccountTrie(updater, getDatabaseStoreFactory());
        setAccountTrie(trie);
        poaMiner.setAccountTrie(trie);

        poAValidator = new PoAValidator();
        poAValidator.setAccountTrie(getAccountTrie());
        setValidator(poAValidator);

        // register dummy account
        getMiner().addListeners(new MinerListener() {
            @Override
            public void onBlockMined(Block block) {
                try {
                    if(peerServer == null) {
                        log.error("mining blocks before server start");
                        return;
                    }
                    peerServer.broadcast(Start.MAPPER.writeValueAsBytes(block));
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onMiningFailed(Block block) {

            }
        });
    }


    @Override
    public void onMessage(Context context, PeerServer server) {

    }

    @Override
    public void onStart(PeerServer server) {
        this.peerServer = server;
    }

    @Override
    public void onNewPeer(Peer peer, PeerServer server) {

    }

    @Override
    public void onDisconnect(Peer peer, PeerServer server) {

    }
}
