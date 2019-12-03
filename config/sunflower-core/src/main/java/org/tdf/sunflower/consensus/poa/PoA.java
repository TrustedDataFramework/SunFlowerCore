package org.tdf.sunflower.consensus.poa;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsMapper;
import org.springframework.core.io.Resource;
import org.tdf.common.*;
import org.tdf.exception.ConsensusEngineLoadException;
import org.tdf.sunflower.Start;
import org.tdf.sunflower.consensus.poa.config.Genesis;
import org.tdf.sunflower.state.Account;
import org.tdf.sunflower.util.FileUtils;

import java.util.Collections;
import java.util.Properties;

import static org.tdf.sunflower.consensus.poa.PoAHashPolicy.HASH_POLICY;

// poa is a minimal non-trivial consensus engine
public class PoA extends ConsensusEngine implements PeerServerListener {
    private PoAConfig poAConfig;
    private Genesis genesis;
    private PoAMiner poaMiner;

    public HashPolicy getPolicy() {
        return HASH_POLICY;
    }

    public PoA() {
        setValidator(new PoAValidator());
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
    public void load(Properties properties, ConsortiumRepository repository) throws ConsensusEngineLoadException {
        JavaPropsMapper mapper = new JavaPropsMapper();
        ObjectMapper objectMapper = new ObjectMapper().enable(JsonParser.Feature.ALLOW_COMMENTS);
        try{
            poAConfig = mapper.readPropertiesAs(properties, PoAConfig.class);
        }catch (Exception e){
            String schema = "";
            try{
                schema = mapper.writeValueAsProperties(new PoAConfig()).toString();
            }catch (Exception ignored){};
            throw new ConsensusEngineLoadException(
                    "load properties failed :" + properties.toString() + " expecting " + schema
            );
        }
        poaMiner = new PoAMiner();
        Resource resource;
        try{
            resource = FileUtils.getResource(poAConfig.getGenesis());
        }catch (Exception e){
            throw new ConsensusEngineLoadException(e.getMessage());
        }
        try{
            genesis = objectMapper.readValue(resource.getInputStream(), Genesis.class);
        }catch (Exception e){
            throw new ConsensusEngineLoadException("failed to parse genesis");
        }
        poaMiner.setPoAConfig(poAConfig);
        poaMiner.setGenesis(genesis);
        poaMiner.setBlockRepository(repository);
        setMiner(poaMiner);
        ConsortiumStateRepository repo = new ConsortiumStateRepository();
        setRepository(repo);
        repo.register(getGenesisBlock(), Collections.singleton(Account.getRandomAccount()));
    }

    @Override
    public ConfirmedBlocksProvider getProvider() {
        return unconfirmed -> unconfirmed;
    }

    @Override
    public PeerServerListener getHandler() {
        return this;
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

    }

    @Override
    public void onDisconnect(Peer peer, PeerServer server) {

    }

    @Override
    public void setTransactionPool(TransactionPool transactionPool) {
        super.setTransactionPool(transactionPool);
        poaMiner.setTransactionPool(transactionPool);
    }
}
