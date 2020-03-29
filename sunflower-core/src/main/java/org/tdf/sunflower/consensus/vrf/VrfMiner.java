package org.tdf.sunflower.consensus.vrf;

import static org.junit.Assert.assertTrue;
import static org.tdf.sunflower.util.ByteUtil.isNullOrZeroArray;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.codec.DecoderException;
import org.bouncycastle.util.encoders.Hex;
import org.tdf.common.trie.Trie;
import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.consensus.AbstractMiner;
import org.tdf.sunflower.consensus.MinerConfig;
import org.tdf.sunflower.consensus.poa.PoAConstants;
import org.tdf.sunflower.consensus.poa.Proposer;
import org.tdf.sunflower.consensus.vrf.contract.VrfBiosContractUpdater;
import org.tdf.sunflower.consensus.vrf.core.BlockIdentifier;
import org.tdf.sunflower.consensus.vrf.core.CommitProof;
import org.tdf.sunflower.consensus.vrf.core.ImportResult;
import org.tdf.sunflower.consensus.vrf.core.ProposalProof;
import org.tdf.sunflower.consensus.vrf.core.StateMachineListener;
import org.tdf.sunflower.consensus.vrf.core.VrfBlockWrapper;
import org.tdf.sunflower.consensus.vrf.core.VrfProof;
import org.tdf.sunflower.consensus.vrf.core.VrfRound;
import org.tdf.sunflower.consensus.vrf.struct.VrfPrivateKey;
import org.tdf.sunflower.consensus.vrf.struct.VrfResult;
import org.tdf.sunflower.consensus.vrf.util.VrfConstants;
import org.tdf.sunflower.consensus.vrf.util.VrfMessageCode;
import org.tdf.sunflower.consensus.vrf.util.VrfUtil;
import org.tdf.sunflower.events.NewBlocksReceived;
import org.tdf.sunflower.exception.ConsensusEngineInitException;
import org.tdf.sunflower.facade.BlockRepository;
import org.tdf.sunflower.facade.TransactionPool;
import org.tdf.sunflower.net.MessageBuilder;
import org.tdf.sunflower.net.PeerServer;
import org.tdf.sunflower.state.Account;
import org.tdf.sunflower.state.Constants;
import org.tdf.sunflower.types.Block;
import org.tdf.sunflower.types.Header;
import org.tdf.sunflower.types.Transaction;
import org.tdf.sunflower.util.ByteUtil;

import com.fasterxml.jackson.core.JsonProcessingException;

import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@Setter
public class VrfMiner extends AbstractMiner {

    private VrfConfig vrfConfig;

    HexBytes minerAddress;

    private VrfGenesis genesis;

    private BlockRepository blockRepository;

    private boolean stopped;

    private Thread thread;

    private VrfStateMachine vrfStateMachine;

    private boolean vrfStarted = false;
    private boolean blockBroadcast;

    private VrfPrivateKey vrfSk;
    private byte[] minerCoinbase;
    private byte[] vrfSeed;
    // Because we have seed checking logic in VrfStateMachine, so VrfStateMachine is
    // aligned to latest VrfRound only in case that its latest block is the same one
    // as other peers.
    // If block number of new mined block is not the same as VrfRound, it tell that
    // this peer synced latest blocks whose block number is larger than its
    // VrfRound.
    // So let it setup new VrfRound from latest block number.
    private boolean blockSyncing;
    private static final int VRF_BLOCK_EARLY_BROADCAST_PRIORITY = 10;

    private PeerServer peerServer;
    private MessageBuilder messageBuilder;

    private TransactionPool transactionPool;
    // contract storage trie
    private Trie<byte[], byte[]> contractStorageTrie;

    public VrfMiner(MinerConfig minerConfig) {
        super(minerConfig);
    }

    public void setGenesis(VrfGenesis genesis) {
        this.genesis = genesis;
    }

    public void setConfig(VrfConfig vrfConfig) throws ConsensusEngineInitException {
        this.vrfConfig = vrfConfig;
        this.minerAddress = HexBytes.fromHex(vrfConfig.getMinerCoinBase());
        this.minerCoinbase = this.minerAddress.getBytes();
        vrfSk = VrfUtil.getVrfPrivateKey(vrfConfig);
        VrfUtil.VRF_PK = Hex.toHexString(vrfSk.getSigner().generatePublicKey().getEncoded());
    }

    public void setRepository(BlockRepository blockRepository) {
        this.blockRepository = blockRepository;
    }

    @Override
    public void start() {
        stopped = false;
        tryMine();
    }

    @Override
    public void stop() {
        stopVrfStateMachine();
        if (thread != null) {
            thread.interrupt();
        }
        stopped = true;
    }

    public Optional<Proposer> getProposer(Block parent, long currentTimeSeconds) {
        if (genesis.miners.size() == 0) {
            return Optional.empty();
        }
        if (parent.getHeight() == 0) {
            return Optional.of(new Proposer(genesis.miners.get(0).address, 0, Long.MAX_VALUE));
        }
        if (parent.getBody() == null || parent.getBody().size() == 0 || parent.getBody().get(0).getTo() == null)
            return Optional.empty();
        HexBytes prev = parent.getBody().get(0).getTo();
        int prevIndex = genesis.miners.stream().map(x -> x.address).collect(Collectors.toList()).indexOf(prev);
        if (prevIndex < 0) {
            return Optional.empty();
        }

        long step = (currentTimeSeconds - parent.getCreatedAt()) / vrfConfig.getBlockInterval() + 1;

        int currentIndex = (int) (prevIndex + step) % genesis.miners.size();
        long endTime = parent.getCreatedAt() + step * vrfConfig.getBlockInterval();
        long startTime = endTime - vrfConfig.getBlockInterval();
        return Optional.of(new Proposer(genesis.miners.get(currentIndex).address, startTime, endTime));
    }

    public void tryMine() {
        if (!vrfConfig.isEnableMining() || stopped) {
            return;
        }
//        String coinBase = vrfConfig.getMinerCoinBase();
//        Block best = blockRepository.getBestBlock();
        startVrfStateMachine();
    }

    public Transaction createCoinBase(long height) {
        Transaction tx = Transaction.builder().version(PoAConstants.TRANSACTION_VERSION)
                .createdAt(System.currentTimeMillis() / 1000).nonce(height).from(HexBytes.EMPTY)
                .amount(EconomicModelImpl.getConsensusRewardAtHeight(height)).payload(HexBytes.EMPTY).to(minerAddress)
                .signature(HexBytes.EMPTY).build();
        return tx;
    }

    @Override
    @SneakyThrows
    protected Header createHeader(Block parent) {
        Header header = Header.builder().version(parent.getVersion()).hashPrev(parent.getHash())
                .transactionsRoot(PoAConstants.ZERO_BYTES).height(parent.getHeight() + 1)
                .createdAt(System.currentTimeMillis() / 1000).build();
//                .payload(VrfConstants.ZERO_BYTES)
//                .hash(new HexBytes(BigEndian.encodeInt64(parent.getHeight() + 1))).build();

        byte[] vrfPk = vrfSk.generatePublicKey().getEncoded();
        byte[] payLoadBytes = VrfUtil.genPayload(header.getHeight(), this.vrfStateMachine.getVrfRound().getRound(),
                vrfSeed, this.minerCoinbase, VrfConstants.ZERO_BYTES.getBytes(), parent.getHash().getBytes(), vrfSk,
                vrfPk, vrfConfig);
        HexBytes payload = HexBytes.fromBytes(payLoadBytes);
        header.setPayload(payload);
        return header;
    }

    public static class EconomicModelImpl {

        private static final long INITIAL_SUPPLY = 20;

        private static final long HALF_PERIOD = 10000000;

        public static long getConsensusRewardAtHeight(long height) {
            long era = height / HALF_PERIOD;
            long reward = INITIAL_SUPPLY;
            for (long i = 0; i < era; i++) {
                reward = reward * 52218182 / 100000000;
            }
            return reward;
        }

    }

    public void setVrfStateMachine(VrfStateMachine vrfStateMachine) {
        this.vrfStateMachine = vrfStateMachine;
        this.vrfStateMachine.setBlockRepository(blockRepository);
        this.vrfStateMachine.setVrfConfig(vrfConfig);
    }

    private synchronized void startVrfStateMachine() {
        if (vrfStarted) {
            log.warn("VrfStateMachine is already started, quit twice start");
            return;
        }

        if (vrfSk == null || isNullOrZeroArray(minerCoinbase)) {
            log.error("Empty vrfSk {} or coinbase {}, quit startVrfStateMachine.",
                    ByteUtil.toHexString(vrfSk.getEncoded()), ByteUtil.toHexString(minerCoinbase));
            return;
        }

        if (vrfStateMachine == null) {
            log.error("Empty vrfStateMachine to setup VrfMiner");
            return;
        }

        final byte[] vrfPk = vrfSk.generatePublicKey().getEncoded();
        if (!vrfStateMachine.registered(minerCoinbase, vrfPk)) {
            log.error(
                    "!!! Miner is not registered as a validator, Please deposit your balance to Native Deposit Contract");
            log.error("Miner has not Deposit to participate in VRF consensus, minerCoinbase 0x{}, vrfPk 0x{}",
                    Hex.toHexString(minerCoinbase, 0, 6), Hex.toHexString(vrfPk, 0, 6));
        }

        log.info("VrfStateMachine is startup, minerCoinbase 0x{}, VrfPk 0x{}", Hex.toHexString(minerCoinbase, 0, 6),
                Hex.toHexString(vrfPk, 0, 6));

        final Block bestPendingState = blockRepository.getBestBlock();

        final long nextBlockNum = bestPendingState.getHeight() + 1;

        if (!setupVrfStateMachine(vrfStateMachine)) {
            log.error("Fail to setup Vrf Miner, quit startVrfStateMachine");

            return;
        }

        vrfStateMachine.start(nextBlockNum);

        vrfStarted = true;
    }

    private synchronized void stopVrfStateMachine() {
        if (!vrfStarted) {
            log.warn("VrfStateMachine is already stopped, quit twice stop");
            return;
        }

        if (vrfSk == null || isNullOrZeroArray(minerCoinbase)) {
            log.error("Empty vrfSk {} or coinbase {}, quit stopVrfStateMachine.",
                    ByteUtil.toHexString(vrfSk.getEncoded()), ByteUtil.toHexString(minerCoinbase));
            return;
        }

        vrfStateMachine.stop();

        vrfStateMachine.cleanupListeners();

        vrfStarted = false;

        log.info("VrfStateMachine is stopped, minerCoinbase 0x{}", Hex.toHexString(minerCoinbase), 0, 6);
    }

    private boolean setupVrfStateMachine(VrfStateMachine vrfStateMachine) {
        StateMachineListener listener = new StateMachineListener() {
            @Override
            public void stateChanged(int from, int to) throws DecoderException, IOException {
                log.info("state changed, State {} -> {}, blockSyncing {}", from, to, blockSyncing);

                if (from == VrfStateMachine.VRF_STATE_INIT && to == VrfStateMachine.VRF_STATE_PROPOSAL) {
                    proposeNewBlock(vrfStateMachine);
                } else if (from == VrfStateMachine.VRF_STATE_PROPOSAL && to == VrfStateMachine.VRF_STATE_REDUCTION) {
                    proposeReductionCommit(vrfStateMachine);
                } else if (from == VrfStateMachine.VRF_STATE_REDUCTION && to == VrfStateMachine.VRF_STATE_FINAL) {
                    proposeFinalCommit(vrfStateMachine);
                } else if (from == VrfStateMachine.VRF_STATE_FINAL && to == VrfStateMachine.VRF_STATE_PROPOSAL) {
                    checkAndProcessNewProposal(vrfStateMachine);
                } else {
                    log.info(">>>, State Align, State {} -> {}, <<<, blockSyncing {}", from, to, blockSyncing);
                }
            }

            @Override
            public boolean finalizeNewBlock(VrfRound vrfRound, VrfBlockWrapper finalBlock) {
                if (finalBlock != null) {
                    final long blockNumber = vrfStateMachine.getVrfRound().getBlockNum();

                    // Save final block to storage
                    if (blockNumber == vrfRound.getBlockNum()) {
                        if (blockSyncing) {
                            blockSyncing = false;
                        }

                        // Clear block header magic before saving it to storage
                        // finalBlock.getHeader().clearMagic();
                        clearMagic(finalBlock.getBlock());

                        ImportResult importResult = tryToConnect(finalBlock.getBlock());// ((BlockchainImpl)
                                                                                        // ethereum.getBlockchain()).tryToConnect(finalBlock.getBlock());
                        if (importResult.isSuccessful()) {
                            log.info("Final Block is Imported as {} one, hash 0x{}",
                                    importResult == ImportResult.IMPORTED_BEST ? "BEST" : "NOT best",
                                    Hex.toHexString(finalBlock.getHash(), 0, 3));
                            return true;
                        } else {
                            log.error("Final Block is NOT Imported, ImportResult {}, hash 0x{}", importResult,
                                    Hex.toHexString(finalBlock.getHash(), 0, 3));
                        }
                    } else {
                        if (finalBlock.getNodeId() != null) {
                            log.warn("In blockSyncing state, add Final Block to sync queue, hash 0x{}",
                                    Hex.toHexString(finalBlock.getHash(), 0, 3));
                            // Clear block header magic before add it to sync queue
//                            finalBlock.getHeader().clearMagic();
                            clearMagic(finalBlock.getBlock());

                            if (validateAndAddNewBlock(finalBlock.getBlock())) {
                                return true;
                            } else {
                                log.error("Fail to do validateAndAddNewBlock for Final Block, hash 0x{}",
                                        Hex.toHexString(finalBlock.getHash(), 0, 3));
                            }
                        } else {
                            log.error("In blockSyncing state, Final Block is a self mined one, PLEASE FIX ME !!!");
                        }
                    }
                }

                return false;
            }
        };
        vrfStateMachine.addListener(listener);

        return true;
    }

    /**
     * New added VRF miner logic for VRF consensus protocol.
     *
     * @author James Hu, silk chain
     * @since 2019/06/27
     */
    private class VrfMined {
        private Block vrfBlock;
        private byte[] vrfSeed;

        public VrfMined(Block vrfBlock, byte[] vrfSeed) {
            this.vrfBlock = vrfBlock;
            this.vrfSeed = vrfSeed;
        }

        public Block getVrfBlock() {
            return vrfBlock;
        }

        public byte[] getVrfSeed() {
            return vrfSeed;
        }
    }

    private class VrfBlockProof {
        private ProposalProof proof;
        private int priority;

        public VrfBlockProof(ProposalProof proof, int priority) {
            this.proof = proof;
            this.priority = priority;
        }

        public ProposalProof getProof() {
            return proof;
        }

        public int getPriority() {
            return priority;
        }
    }

    private VrfMined getNewVrfMined() throws DecoderException, IOException {
        Block bestBlock = blockRepository.getBestBlock();
        Block bestPendingState = blockRepository.getBestBlock();
        vrfSeed = VrfUtil.getSeed(bestPendingState);

        log.debug("getNewBlockForMining best blocks: PendingState: " + bestPendingState.getHeight() + ", Blockchain: "
                + bestBlock.getHeight());

        Block newMiningBlock = createBlock(bestBlock).get();// blockchain.createNewBlock(bestPendingState,
        // getAllPendingTransactions(), getUncles(bestPendingState));
        log.info("######## Get new block for mining: #" + newMiningBlock.getHeight());

        VrfMined vrfMined = new VrfMined(newMiningBlock, vrfSeed);

        return vrfMined;
    }

    public VrfBlockProof createProposalProofAndUpdateBlock(long blockNum, int round, Block newBlock) {
        if (isNullOrZeroArray(vrfSeed)) {
            log.debug("Empty vrfSeed in createProposalProofAndUpdateBlock");
            return null;
        }

        if (isNullOrZeroArray(minerCoinbase)) {
            log.error("Empty coinbase in createProposalProofAndUpdateBlock");
            return null;
        }

        byte[] vrfPk = vrfSk.generatePublicKey().getEncoded();

        // Must use VrfProof Util to proof with Role Code
        VrfResult vrfResult = VrfProof.Util.prove(VrfProof.ROLE_CODES_PROPOSER, round, vrfSk, vrfSeed);
        VrfProof vrfProof = VrfProof.Util.vrfProof(VrfProof.ROLE_CODES_PROPOSER, round, vrfPk, vrfSeed, vrfResult);

        // Update block nonce to header, and it will change valuf of block hash
        VrfUtil.setSeed(newBlock, vrfResult.getR());

        // Setup block identifier
        byte[] identifier = newBlock.getHash().getBytes();
        BlockIdentifier blockIdentifier = new BlockIdentifier(identifier, blockNum);

        ProposalProof proof = new ProposalProof(vrfProof, minerCoinbase, blockIdentifier, vrfSk.getSigner());

        // Check priority of new proof
        int priority = vrfStateMachine.getValidPriority(proof);
        if (priority > 0) {
            // Update priority field.
            log.info("Set priority {} to block #{}", priority, newBlock.getHeader().getHeight());
            VrfUtil.setPriority(newBlock, ByteUtil.longToBytes(priority));
            // Update identifier after header changed, because block hash changed as
            // difficulty changed
            identifier = newBlock.getHash().getBytes();
            blockIdentifier = new BlockIdentifier(identifier, blockNum);
            proof = new ProposalProof(vrfProof, minerCoinbase, blockIdentifier, vrfSk.getSigner());

            log.info("New Proposal Proof, priority {}, hash 0x{}, identifier {}", priority,
                    Hex.toHexString(proof.getHash(), 0, 3), proof.getBlockIdentifier());
            // Set proposal proof to block header, and it will not change value of block
            // hash
            VrfUtil.setProposalProof(newBlock, proof);

            VrfBlockProof blockProof = new VrfBlockProof(proof, priority);
            return blockProof;
        }

        return null;
    }

    public CommitProof createReductionCommitProof(long blockNum, int round, BlockIdentifier blockIdentifier) {
        if (isNullOrZeroArray(vrfSeed)) {
            log.debug("Empty vrfSeed in createReductionCommitProof");
            return null;
        }

        if (isNullOrZeroArray(minerCoinbase)) {
            log.error("Empty coinbase in createReductionCommitProof");
            return null;
        }

        byte[] vrfPk = vrfSk.generatePublicKey().getEncoded();

        // Must use VrfProof Util to proof with Role Code
        VrfResult vrfResult = VrfProof.Util.prove(VrfProof.ROLE_CODES_REDUCTION_COMMIT, round, vrfSk, vrfSeed);
        VrfProof vrfProof = VrfProof.Util.vrfProof(VrfProof.ROLE_CODES_REDUCTION_COMMIT, round, vrfPk, vrfSeed,
                vrfResult);

        CommitProof proof = new CommitProof(vrfProof, minerCoinbase, blockIdentifier, vrfSk.getSigner());

        // Check priority of new proof
        int priority = vrfStateMachine.getValidPriority(proof);
        if (priority > 0) {
            log.info("New Commit Proof, priority {}, hash 0x{}, identifier {}", priority,
                    Hex.toHexString(proof.getHash(), 0, 3), proof.getBlockIdentifier());
            return proof;
        }

        return null;
    }

    public CommitProof createFinalCommitProof(long blockNum, int round, BlockIdentifier blockIdentifier) {
        if (isNullOrZeroArray(vrfSeed)) {
            log.debug("Empty vrfSeed in createFinalCommitProof");
            return null;
        }

        if (isNullOrZeroArray(minerCoinbase)) {
            log.error("Empty coinbase in createFinalCommitProof");
            return null;
        }

        byte[] vrfPk = vrfSk.generatePublicKey().getEncoded();

        // Must use VrfProof Util to proof with Role Code
        VrfResult vrfResult = VrfProof.Util.prove(VrfProof.ROLE_CODES_FINAL_COMMIT, round, vrfSk, vrfSeed);
        VrfProof vrfProof = VrfProof.Util.vrfProof(VrfProof.ROLE_CODES_FINAL_COMMIT, round, vrfPk, vrfSeed, vrfResult);

        CommitProof proof = new CommitProof(vrfProof, minerCoinbase, blockIdentifier, vrfSk.getSigner());

        // Check priority of new proof
        int priority = vrfStateMachine.getValidPriority(proof);
        if (priority > 0) {
            log.info("New Final Proof, priority {}, hash 0x{}, identifier {}", priority,
                    Hex.toHexString(proof.getHash(), 0, 3), proof.getBlockIdentifier());
            return proof;
        }

        return null;
    }

    private void proposeNewBlock(VrfStateMachine vrfStateMachine) throws DecoderException, IOException {
        VrfRound vrfRound = vrfStateMachine.getVrfRound();
        long blockNum = vrfRound.getBlockNum();
        int round = vrfRound.getRound();

        // Try to get a new block
        VrfMined vrfMined = getNewVrfMined();
        Block newBlock = vrfMined.getVrfBlock();

        final long minedBlockNum = newBlock.getHeader().getHeight();

        // Because we have seed checking logic in VrfStateMachine, so VrfStateMachine is
        // aligned to latest VrfRound only in case that its latest block is the same one
        // as other peers.
        // If block number of new mined block is not the same as VrfRound, it tell that
        // this peer synced latest blocks whose block number is larger than its
        // VrfRound.
        // So let it setup new VrfRound from latest block number.
        if (minedBlockNum > blockNum) {
            log.error(
                    "New mined block number is larger than VrfRound told, Setup new VrfRound: #{},{} <- #{},{} for starting its own VRF consensus logic in blockSyncing state",
                    minedBlockNum, 0, blockNum, round);

            // setup VrfRound as new mined block told
            vrfRound.setBlockNum(vrfStateMachine, minedBlockNum);
            vrfRound.setRound(vrfStateMachine, 0);

            blockNum = minedBlockNum;
            round = 0;

            blockSyncing = true;
        } else if (minedBlockNum < blockNum) {
            log.error(
                    "FATAL ERROR, New mined block number is smaller than VrfRound told, #{} <- #{}, quit from proposal proof",
                    minedBlockNum, blockNum);
            return;
        }

        // setup vrf seed
        vrfSeed = vrfMined.getVrfSeed();

        // setup proposal proof and broadcast it to other pees
        VrfBlockProof blockProof = createProposalProofAndUpdateBlock(blockNum, round, newBlock);
        if (blockProof != null) {
            // Add proposal to VrfStateMachine
            ProposalProof proposalProof = blockProof.getProof();
            if (vrfStateMachine.addProof(proposalProof, false)) {
                log.info("Try to broadcast a new proposal for block, block header {}",
                        newBlock.getHeader().getHash().toHex());

                // Broadcast proposal to other peers
                // ethereum.getChannelManager().sendProposalProof(proposalProof, null);
                sendProposalProof(proposalProof);

                // Add new block to state machine
                VrfBlockWrapper blockWrapper = new VrfBlockWrapper(newBlock, null);
                if (!vrfStateMachine.addNewBlock(blockWrapper)) {
                    log.info("Fail to add new proposed block to Vrf State Machine, hash 0x{}",
                            Hex.toHexString(newBlock.getHash().getBytes(), 0, 3));
                }

                // Check if we can broadcast new block earlier
                if (blockProof.getPriority() >= VRF_BLOCK_EARLY_BROADCAST_PRIORITY) {
                    log.info("Try to broadcast new block, #{}, hash 0x{}, priority {}", newBlock.getHeight(),
                            Hex.toHexString(newBlock.getHash().getBytes(), 0, 3), blockProof.getPriority());
                    // Broadcast block to other peers as proposed one
                    if (setupMagic(newBlock)) {
                        // ethereum.getChannelManager().sendNewBlock(newBlock);
                        sendNewBlock(newBlock);
                        blockBroadcast = true;
                    } else {
                        log.error("Fail to setup magic for new proposed block, give up broadcast");
                        blockBroadcast = false;
                    }
                } else {
                    blockBroadcast = false;
                }
            } else {
                log.error("Fail to add proposal proof to Vrf State Machine");
            }
        } else {
            log.warn("Not get proposal proof, give up Proposal Prove");
        }
    }

    // Try to commit for the best proposal of block identifier without block body
    // received.
    // Only to confirm high priority among all of the validators.
    private void proposeReductionCommit(VrfStateMachine vrfStateMachine) {
        VrfRound vrfRound = vrfStateMachine.getVrfRound();
        long blockNum = vrfRound.getBlockNum();
        int round = vrfRound.getRound();

        ProposalProof bestProproserProof = vrfStateMachine.getBestProposalProof();
        if (bestProproserProof != null && bestProproserProof.getBlockIdentifier() != null) {
            try {
                assertTrue(bestProproserProof.getBlockIdentifier().getNumber() == blockNum);

                // Check if winner block is own one, and it also is the best one, do broadcast
                VrfBlockWrapper winnerBlock = vrfStateMachine.getWinnerBlock();
                if (!blockBroadcast && winnerBlock != null) {
                    if (Arrays.equals(winnerBlock.getCoinbase(), minerCoinbase)
                            && Arrays.equals(bestProproserProof.getCoinbase(), minerCoinbase)) {
                        log.info("Try to broadcast my own new best block, hash 0x{}",
                                Hex.toHexString(winnerBlock.getHash(), 0, 6));
                        // Broadcast block to other peers as new proposal block
                        if (setupMagic(winnerBlock.getBlock())/* winnerBlock.getHeader().setupMagic() */) {
                            // ethereum.getChannelManager().sendNewBlock(winnerBlock.getBlock());
                            sendNewBlock(winnerBlock.getBlock());
                            blockBroadcast = true;
                        } else {
                            log.error("Fail to setup magic for new proposed block, give up broadcast, blockSyncing {}",
                                    blockSyncing);
                        }
                    }
                }

                BlockIdentifier blockIdentifier = bestProproserProof.getBlockIdentifier();
                CommitProof commitProof = createReductionCommitProof(blockNum, round, blockIdentifier);
                if (commitProof != null) {
                    // Add commit to VrfStateMachine
                    if (vrfStateMachine.addProof(commitProof, false)) {
                        // Broadcast commit to other peers
//                        ethereum.getChannelManager().sendCommitProof(commitProof, null);
                        sendCommitProof(commitProof);
                    } else {
                        log.error("Fail to add reduction commit proof to Vrf State Machine, blockSyncing {}",
                                blockSyncing);
                    }
                } else {
                    log.warn("Not get reduction commit proof, give up Proposal Comit Prove, blockSyncing {}",
                            blockSyncing);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            log.warn("Not get the best proposal proof, give up Proposal Commit Prove, blockSyncing {}", blockSyncing);
        }
    }

    // Try to check committed proposal and block body.
    // If block body is matched with committed proposal, do the final commit
    private void proposeFinalCommit(VrfStateMachine vrfStateMachine) throws JsonProcessingException {
        VrfRound vrfRound = vrfStateMachine.getVrfRound();
        long blockNum = vrfRound.getBlockNum();
        int round = vrfRound.getRound();

        // Check if candidate block is the committed one
        VrfBlockWrapper winnerBlock = vrfStateMachine.getWinnerBlock();
        BlockIdentifier reachCommitIdentifier = vrfStateMachine.reachCommitBlockIdentifier();
        if (reachCommitIdentifier == null) {
            log.error("NOT reach reduction commit agreement, give up Final Commit Proof, blockSyncing {}",
                    blockSyncing);
        } else if (winnerBlock == null) {
            log.error("Empty winner block, give up Final Commit Proof, reach commit identifier {}, blockSyncing {}",
                    reachCommitIdentifier, blockSyncing);

            // !!!!!!!!!!! -----> Need to fix, should not be true.
//        } else if (!Arrays.equals(winnerBlock.getHash(), reachCommitIdentifier.getHash())
//                || winnerBlock.getNumber() != reachCommitIdentifier.getNumber()) {
//            log.error(
//                    "Identifier is NOT matched, winner block {}, committed identifier {}, give up Final Commit Proof, blockSyncing {}",
//                    Hex.toHexString(winnerBlock.getHash(), 0, 6),
//                    Hex.toHexString(reachCommitIdentifier.getHash(), 0, 6), blockSyncing);
//            log.error("PLEASE configure VrfStateMachine for longer broadcast time of new block in the network");
        } else {

            if (!Arrays.equals(winnerBlock.getHash(), reachCommitIdentifier.getHash())
                    || winnerBlock.getNumber() != reachCommitIdentifier.getNumber()) {
                log.error(
                        "Identifier is NOT matched, winner block {}, committed identifier {}, give up Final Commit Proof, blockSyncing {} -----> Need to fix, should not be true.",
                        Hex.toHexString(winnerBlock.getHash(), 0, 6),
                        Hex.toHexString(reachCommitIdentifier.getHash(), 0, 6), blockSyncing);
                log.error("PLEASE configure VrfStateMachine for longer broadcast time of new block in the network");
            }

            // Check the best proposal and committed proposal
            ProposalProof bestProproserProof = vrfStateMachine.getBestProposalProof();
            if (!Arrays.equals(bestProproserProof.getBlockIdentifier().getHash(), reachCommitIdentifier.getHash())
                    || bestProproserProof.getBlockIdentifier().getNumber() != reachCommitIdentifier.getNumber()) {
                log.warn("Not matched between best proposal and committed proposal, blockSyncing {}", blockSyncing);
            }

            // Submit Final Commit
            CommitProof finalProof = createFinalCommitProof(blockNum, round, reachCommitIdentifier);
            if (finalProof != null) {
                // Add final to VrfStateMachine
                if (vrfStateMachine.addProof(finalProof, false)) {
                    // Broadcast final to other peers
                    // ethereum.getChannelManager().sendCommitProof(finalProof, null);
                    sendCommitProof(finalProof);
                } else {
                    log.error("Fail to add final commit proof to Vrf State Machine, blockSyncing {}", blockSyncing);
                }
            } else {
                log.warn("Not get final commit proof, give up Final Commit Prove, blockSyncing {}", blockSyncing);
            }
        }
    }

    private void checkAndProcessNewProposal(VrfStateMachine vrfStateMachine) throws DecoderException, IOException {
        VrfRound vrfRound = vrfStateMachine.getVrfRound();
        long blockNum = vrfRound.getBlockNum();
        int round = vrfRound.getRound();

        log.info("new VrfRound is arrived for Vrf Miner: <{}>, <{}>", blockNum, round);
        if (round == 0) {
            log.info("!!! Reach VRF consensus agreement, try to check Final Block #{} and propose a new block",
                    blockNum - 1);

            VrfBlockWrapper finalBlock = vrfStateMachine.getFinalBlock();

            if (finalBlock == null) {
                // Two cases:
                // 1. State aligned, we lost final block
                // 2. We could not get the final commit block from network in time.
                log.error(
                        "Fail to get #{} Final Block, Waiting for it being synced, Try to check block broadcast time, blockSyncing {}",
                        blockNum - 1, blockSyncing);
            }
        } else {
            log.info("### NOT reach VRF consensus agreement, try to propose a new block in next round");
        }

        // Anyway, try to propose a new block
        proposeNewBlock(vrfStateMachine);
    }

    private boolean setupMagic(Block block) {
        return true;
    }

    private void sendNewBlock(Block block) throws JsonProcessingException {
//        byte[] encoded = RLPSerializer.SERIALIZER.serialize(block);
//        Message message = messageBuilder.buildMessage(Code.VRF_BLOCK, VrfConstants.MESSAGE_TTL, encoded);
//        peerServer.broadcast(message.toByteArray());

        byte[] encoded = VrfUtil.buildMessageBytes(VrfMessageCode.VRF_BLOCK, block);
        peerServer.broadcast(encoded);
    }

    private void sendProposalProof(ProposalProof prosalProof) throws JsonProcessingException {
//        byte[] encoded = RLPSerializer.SERIALIZER.serialize(prosalProof);
//        Message message = messageBuilder.buildMessage(Code.VRF_PROPOSAL_PROOF, VrfConstants.MESSAGE_TTL, encoded);
//        peerServer.broadcast(message.toByteArray());
        byte[] encoded = VrfUtil.buildMessageBytes(VrfMessageCode.VRF_PROPOSAL_PROOF, prosalProof);
        peerServer.broadcast(encoded);
    }

    private void sendCommitProof(CommitProof commitProof) throws JsonProcessingException {
//        byte[] encoded = RLPSerializer.SERIALIZER.serialize(commitProof);
//        Message message = messageBuilder.buildMessage(Code.VRF_COMMIT_PROOF, VrfConstants.MESSAGE_TTL, encoded);
//        peerServer.broadcast(message.toByteArray());
        byte[] encoded = VrfUtil.buildMessageBytes(VrfMessageCode.VRF_COMMIT_PROOF, commitProof);
        peerServer.broadcast(encoded);
    }

    private boolean validateAndAddNewBlock(Block block) {
        return true;
    }

    private void clearMagic(Block block) {

    }

    private ImportResult tryToConnect(Block block) {
        // Notify listeners.
        getEventBus().publish(new NewBlocksReceived(Collections.singletonList(block)));
        return ImportResult.IMPORTED_BEST;
    }

    public long getCollateral(String address) {
        Optional<Account> accountOpt = this.getAccountTrie().get(
                blockRepository.getBestBlock().getStateRoot().getBytes(), Constants.VRF_BIOS_CONTRACT_ADDR_HEX_BYTES);

        if (!accountOpt.isPresent()) {
            return 0;
        }

        Account account = accountOpt.get();
        Trie<byte[], byte[]> trie = contractStorageTrie.revert(account.getStorageRoot());

        Optional<byte[]> collateralOpt = trie.get(HexBytes.fromHex(address).getBytes());

        if (!collateralOpt.isPresent()) {
            return 0;
        }

        long collateral = ByteUtil.byteArrayToLong(collateralOpt.get());
        return collateral;
    }
}
