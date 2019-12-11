package org.tdf.sunflower.consensus.vrf;

import static org.tdf.sunflower.consensus.vrf.VrfHashPolicy.HASH_POLICY;

import java.util.Collections;
import java.util.Optional;
import java.util.Properties;

import org.springframework.core.io.Resource;
import org.tdf.common.Block;
import org.tdf.common.ConfirmedBlocksProvider;
import org.tdf.common.ConsensusEngine;
import org.tdf.common.ConsortiumRepository;
import org.tdf.common.ConsortiumStateRepository;
import org.tdf.common.Context;
import org.tdf.common.HashPolicy;
import org.tdf.common.MinerListener;
import org.tdf.common.Peer;
import org.tdf.common.PeerServer;
import org.tdf.common.PeerServerListener;
import org.tdf.exception.ConsensusEngineLoadException;
import org.tdf.rlp.RLPDeserializer;
import org.tdf.serialize.RLPSerializer;
import org.tdf.sunflower.consensus.vrf.core.CommitProof;
import org.tdf.sunflower.consensus.vrf.core.PendingVrfState;
import org.tdf.sunflower.consensus.vrf.core.ProposalProof;
import org.tdf.sunflower.consensus.vrf.core.ValidatorManager;
import org.tdf.sunflower.consensus.vrf.core.VrfBlockWrapper;
import org.tdf.sunflower.consensus.vrf.util.VrfConstants;
import org.tdf.sunflower.consensus.vrf.util.VrfMessageCode;
import org.tdf.sunflower.consensus.vrf.util.VrfUtil;
import org.tdf.sunflower.consensus.vrf.util.VrfUtil.VrfMessageCodeAndBytes;
import org.tdf.sunflower.net.MessageBuilder;
import org.tdf.sunflower.net.PeerImpl;
import org.tdf.sunflower.proto.Code;
import org.tdf.sunflower.proto.Message;
import org.tdf.sunflower.state.Account;
import org.tdf.sunflower.util.ByteUtil;
import org.tdf.sunflower.util.FileUtils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsMapper;
import com.google.protobuf.InvalidProtocolBufferException;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Setter
@Slf4j
public class VrfEngine extends ConsensusEngine implements PeerServerListener {
    private VrfGenesis genesis;
    private VrfConfig vrfConfig;
    private VrfStateMachine vrfStateMachine;
    private MessageBuilder messageBuilder;

    private PeerServer peerServer;
    private VrfMiner vrfMiner;
    private ConsortiumRepository blockRepository;

    public VrfEngine() {
        setValidator(new VrfValidator());
    }

    @Override
    public void onMessage(Context context, PeerServer server) {
        byte[] messageBytes = context.getMessage();
        VrfMessageCodeAndBytes codeAndBytes = VrfUtil.parseMessageBytes(messageBytes);
        VrfMessageCode code = codeAndBytes.getCode();
        byte[] vrfBytes = codeAndBytes.getRlpBytes();

        switch (code) {
        case VRF_BLOCK:
            processVrfBlockMsg(context, vrfBytes);
            break;
        case VRF_PROPOSAL_PROOF:
            processVrfProposalProofMsg(vrfBytes);
            break;
        case VRF_COMMIT_PROOF:
            processCommitProofMsg(vrfBytes);
            break;
        case NEW_MINED_BLOCK:
            processNewMinedBlockMsg(vrfBytes);
            break;
        }
    }

    private void processNewMinedBlockMsg(byte[] bodyBytes) {
        Block blockNew = RLPDeserializer.deserialize(bodyBytes, Block.class);
        log.info("New mined block received from peer. Num #{}, Hash {}", blockNew.getHeight(),
                blockNew.getHash().toString());
        saveBlock(blockNew, blockRepository, this);
    }

    private void processCommitProofMsg(byte[] bodyBytes) {
        CommitProof commitProof = RLPDeserializer.deserialize(bodyBytes, CommitProof.class);
        log.info("VRF CommitProof received. Round #{}, miner {}, block hash {}", commitProof.getRound(),
                commitProof.getCoinbase(), ByteUtil.toHexString(commitProof.getBlockIdentifier().getHash()));
        vrfStateMachine.addProof(commitProof, true);
    }

    private void processVrfProposalProofMsg(byte[] bodyBytes) {
        ProposalProof proposalProof = RLPDeserializer.deserialize(bodyBytes, ProposalProof.class);
        log.info("VRF ProposalProof received. Round #{}, miner {}, block hash {}", proposalProof.getRound(),
                proposalProof.getCoinbase(), ByteUtil.toHexString(proposalProof.getBlockIdentifier().getHash()));
        vrfStateMachine.addProof(proposalProof, true);
    }

    private void processVrfBlockMsg(Context context, byte[] bodyBytes) {
        Block block = RLPDeserializer.deserialize(bodyBytes, Block.class);
        log.info("VRF block received. Num #{}, Hash {}", block.getHeight(), block.getHash().toString());
        vrfStateMachine.addNewBlock(new VrfBlockWrapper(block, context.getRemote().getID().getBytes()));
    }

    @Override
    public void onStart(PeerServer server) {

        setPeerServer(server);
        setMessageBuilder(new MessageBuilder((PeerImpl) peerServer.getSelf()));
        vrfMiner.setPeerServer(peerServer);
        vrfMiner.setMessageBuilder(messageBuilder);

        getMiner().addListeners(new MinerListener() {
            @Override
            public void onBlockMined(Block block) {
                log.info("!!! Wow, new block mined. #{}, {}", block.getHeight(),
                        ByteUtil.toHexString(block.getHash().getBytes()));
//                byte[] encoded = RLPSerializer.SERIALIZER.serialize(block);
//                Message message = messageBuilder.buildMessage(Code.NEW_MINED_BLOCK, VrfConstants.MESSAGE_TTL, encoded);
                byte[] encoded = VrfUtil.buildMessageBytes(VrfMessageCode.NEW_MINED_BLOCK, block);
                peerServer.broadcast(encoded);
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
        try {
            vrfConfig = mapper.readPropertiesAs(properties, VrfConfig.class);
        } catch (Exception e) {
            String schema = "";
            try {
                schema = mapper.writeValueAsProperties(new VrfConfig()).toString();
            } catch (Exception ignored) {
            }
            ;
            throw new ConsensusEngineLoadException(
                    "load properties failed :" + properties.toString() + " expecting " + schema);
        }
        vrfMiner = new VrfMiner();
        Resource resource;
        try {
            resource = FileUtils.getResource(vrfConfig.getGenesis());
        } catch (Exception e) {
            throw new ConsensusEngineLoadException(e.getMessage());
        }
        try {
            genesis = objectMapper.readValue(resource.getInputStream(), VrfGenesis.class);
        } catch (Exception e) {
            throw new ConsensusEngineLoadException("failed to parse genesis: " + e.getMessage());
        }

        setBlockRepository(repository);

        vrfMiner.setConfig(vrfConfig);
        vrfMiner.setGenesis(genesis);
        vrfMiner.setRepository(repository);

        // ------- Need well implementation.
        ValidatorManager validatorManager = new ValidatorManager(repository);
        vrfStateMachine = new VrfStateMachine(validatorManager, new PendingVrfState(validatorManager),
                new VrfValidator());
        vrfMiner.setVrfStateMachine(vrfStateMachine);

        setMiner(vrfMiner);

        setRepository(new ConsortiumStateRepository());

        // register miner accounts
        getRepository().register(getGenesisBlock(), Collections.singleton(new Account(vrfMiner.minerPublicKeyHash, 0)));

    }

    @Override
    public Block getGenesisBlock() {
        Block genesisBlock = super.getGenesisBlock();
        if (genesisBlock != null)
            return genesisBlock;
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

    private boolean saveBlock(Block block, ConsortiumRepository repository, ConsensusEngine engine) {
        Optional<Block> o = repository.getBlock(block.getHashPrev().getBytes());
        if (!o.isPresent())
            return false;
        if (engine.getValidator().validate(block, o.get()).isSuccess()) {
            repository.writeBlock(block);
        }
        return true;
    }
}
