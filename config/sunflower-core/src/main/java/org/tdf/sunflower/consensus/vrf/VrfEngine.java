package org.tdf.sunflower.consensus.vrf;

import java.util.Collections;
import java.util.Properties;

import org.springframework.core.io.Resource;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsMapper;
import org.tdf.common.*;
import org.tdf.exception.ConsensusEngineLoadException;
import org.tdf.sunflower.Start;
import org.tdf.sunflower.consensus.vrf.core.PendingVrfState;
import org.tdf.sunflower.consensus.vrf.core.ValidatorManager;
import org.tdf.sunflower.state.Account;
import org.tdf.sunflower.util.FileUtils;

import static org.tdf.sunflower.consensus.vrf.VrfHashPolicy.HASH_POLICY;

public class VrfEngine extends ConsensusEngine implements PeerServerListener {
    private VrfGenesis genesis;
    private VrfConfig vrfConfig;
    private VrfStateMachine vrfStateMachine;

    private BlockRepository repository;

    public VrfEngine() {
        setValidator(new VrfValidator());
    }

    @Override
    public void onMessage(Context context, PeerServer server) {

    }

    @Override
    public void onStart(PeerServer server) {
        getMiner().addListeners(new MinerListener() {
            @Override
            public void onBlockMined(Block block) {
                try {
                    server.broadcast(Start.MAPPER.writeValueAsBytes(block));
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
    public void onNewPeer(Peer peer, PeerServer server) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void onDisconnect(Peer peer, PeerServer server) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void load(Properties properties, ConsortiumRepository repository) throws ConsensusEngineLoadException {
        JavaPropsMapper mapper = new JavaPropsMapper();
        ObjectMapper objectMapper = new ObjectMapper().enable(JsonParser.Feature.ALLOW_COMMENTS);
        try{
            vrfConfig = mapper.readPropertiesAs(properties, VrfConfig.class);
        }catch (Exception e){
            String schema = "";
            try{
                schema = mapper.writeValueAsProperties(new VrfConfig()).toString();
            }catch (Exception ignored){};
            throw new ConsensusEngineLoadException(
                    "load properties failed :" + properties.toString() + " expecting " + schema
            );
        }
        VrfMiner vrfMiner = new VrfMiner();
        Resource resource;
        try{
            resource = FileUtils.getResource(vrfConfig.getGenesis());
        }catch (Exception e){
            throw new ConsensusEngineLoadException(e.getMessage());
        }
        try{
            genesis = objectMapper.readValue(resource.getInputStream(), VrfGenesis.class);
        }catch (Exception e){
            throw new ConsensusEngineLoadException("failed to parse genesis: " + e.getMessage());
        }
        vrfMiner.setConfig(vrfConfig);
        vrfMiner.setGenesis(genesis);
        vrfMiner.setRepository(repository);

        // ------- Need well implementation.
        ValidatorManager validatorManager = new ValidatorManager(repository);
        vrfStateMachine = new VrfStateMachine(validatorManager, new PendingVrfState(validatorManager), new VrfValidator());
        vrfMiner.setVrfStateMachine(vrfStateMachine);

        setMiner(vrfMiner);

        setRepository(new ConsortiumStateRepository());

        // register miner accounts
        getRepository().register(getGenesisBlock(), Collections.singleton(new Account(vrfMiner.minerPublicKeyHash, 0)));

    }

    @Override
    public Block getGenesisBlock() {
        Block genesisBlock = super.getGenesisBlock();
        if (genesisBlock != null) return genesisBlock;
        genesisBlock = genesis.getBlock();
        setGenesisBlock(genesisBlock);
        return genesisBlock;
    }

    @Override
    public ConfirmedBlocksProvider getProvider() {
        return unconfirmed -> unconfirmed;
    }

    @Override
    public HashPolicy getPolicy() {
        return HASH_POLICY;
    }

    @Override
    public PeerServerListener getHandler() {
        return this;
    }

}
