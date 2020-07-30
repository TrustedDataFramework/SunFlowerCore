package org.tdf.sunflower.consensus.poa;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.tdf.common.util.HexBytes;
import org.tdf.rlp.RLPCodec;
import org.tdf.sunflower.consensus.AbstractMiner;
import org.tdf.sunflower.consensus.EconomicModel;
import org.tdf.sunflower.consensus.Proposer;
import org.tdf.sunflower.consensus.poa.config.Genesis;
import org.tdf.sunflower.facade.AbstractConsensusEngine;
import org.tdf.sunflower.facade.PeerServerListener;
import org.tdf.sunflower.state.*;
import org.tdf.sunflower.types.Block;
import org.tdf.sunflower.types.Header;
import org.tdf.sunflower.util.FileUtils;
import org.tdf.sunflower.util.MappingUtil;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

// poa is a minimal non-trivial consensus engine
@Slf4j(topic = "poa")
public class PoA extends AbstractConsensusEngine {
    private PoAConfig poAConfig;
    private Genesis genesis;
    private PoAMiner poaMiner;
    private PoAValidator poAValidator;
    private Authentication authContract;
    private Authentication minerContract;
    private List<PreBuiltContract> preBuiltContracts;
    EconomicModel economicModel;


    static byte[] getSignaturePlain(Block b) {
        Header nh = b.getHeader().clone();
        nh.setPayload(HexBytes.EMPTY);
        return RLPCodec.encode(nh);
    }

    public PoA() {
        this.preBuiltContracts = new ArrayList<>();
        this.economicModel = new PoAEconomicModel();
    }

    public PoA(List<PreBuiltContract> preBuiltContracts) {
        this(preBuiltContracts, new PoAEconomicModel());
    }

    public PoA(List<PreBuiltContract> preBuiltContracts, @NonNull EconomicModel economicModel) {
        this();
        this.preBuiltContracts.addAll(preBuiltContracts);
        this.economicModel = economicModel;
    }

    @Override
    public Optional<Set<HexBytes>> getApprovedNodes() {
        Block best = getSunflowerRepository().getBestBlock();
        return Optional.of(new HashSet<>(this.authContract.getNodes(best.getStateRoot().getBytes())));
    }

    @Override
    public List<HexBytes> getMinerAddresses() {
        return getMinerAddresses(getSunflowerRepository().getBestBlock().getStateRoot().getBytes());
    }

    public List<HexBytes> getMinerAddresses(byte[] parentStateRoot) {
        return minerContract.getNodes(parentStateRoot);
    }

    public Optional<Proposer> getProposer(Block parent, long currentEpochSeconds) {
        List<HexBytes> minerAddresses = getMinerAddresses(parent.getStateRoot().getBytes());
        return AbstractMiner.getProposer(parent, currentEpochSeconds, minerAddresses, poAConfig.getBlockInterval());
    }

    @Override
    public List<Account> getGenesisStates() {
        return genesis.alloc == null ? Collections.emptyList() :
                genesis.alloc.entrySet().stream()
                        .map(e -> new Account(HexBytes.fromHex(e.getKey()), e.getValue())).collect(Collectors.toList());
    }

    @Override
    public List<PreBuiltContract> getPreBuiltContracts() {
        return preBuiltContracts;
    }

    @Override
    public void init(Properties properties) {
        ObjectMapper objectMapper = new ObjectMapper()
                .enable(JsonParser.Feature.ALLOW_COMMENTS)
                ;
        poAConfig = MappingUtil.propertiesToPojo(properties, PoAConfig.class);
        InputStream in;
        try {
            in = FileUtils.getInputStream(poAConfig.getGenesis());
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
        try {
            genesis = objectMapper.readValue(in, Genesis.class);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("failed to parse genesis");
        }

        setGenesisBlock(genesis.getBlock());

        setPeerServerListener(PeerServerListener.NONE);
        // create state repository


        this.authContract = new Authentication(
                genesis.miners == null ? Collections.emptyList() :
                        genesis.miners.stream().map(Genesis.MinerInfo::getAddress).collect(Collectors.toSet()),
                Constants.PEER_AUTHENTICATION_ADDR
        );

        this.minerContract = new Authentication(genesis.miners == null ? Collections.emptyList() :
                genesis.miners.stream().map(Genesis.MinerInfo::getAddress).collect(Collectors.toSet()),
                Constants.POA_AUTHENTICATION_ADDR
        );

        preBuiltContracts.add(this.authContract);
        preBuiltContracts.add(this.minerContract);

        initStateTrie();

        StateTrie<HexBytes, Account> trie = getAccountTrie();

        this.authContract.setAccountTrie(trie);
        this.authContract.setContractStorageTrie(getContractStorageTrie());

        this.minerContract.setAccountTrie(trie);
        this.minerContract.setContractStorageTrie(getContractStorageTrie());

        poaMiner = new PoAMiner(getAccountTrie(), getEventBus(), poAConfig, this);
        poaMiner.setBlockRepository(this.getSunflowerRepository());
        poaMiner.setPoAConfig(poAConfig);
        poaMiner.setTransactionPool(getTransactionPool());

        setMiner(poaMiner);

        poAValidator = new PoAValidator(getAccountTrie(), this);
        setValidator(poAValidator);

        // register dummy account
    }

    @Override
    public Object rpcQuery(HexBytes address, JsonNode body) {
        byte[] root = getSunflowerRepository().getBestBlock().getStateRoot().getBytes();
        String method = body == null ? null : body.get("method").asText();
        if (address.equals(Constants.POA_AUTHENTICATION_ADDR) || address.equals(Constants.PEER_AUTHENTICATION_ADDR)) {
            Authentication auth = address.equals(Constants.POA_AUTHENTICATION_ADDR) ?
                    minerContract : authContract;
            switch (Objects.requireNonNull(method)) {
                case "nodes":
                    return auth.getNodes(root);
                case "pending":
                    return auth.getPending(root);
                default:
                    throw new RuntimeException(method + " not defined");
            }
        }
        return UNRESOLVED;
    }

    @Override
    public String getName() {
        return "poa";
    }
}
