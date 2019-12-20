/*
 * Copyright (c) [2019] [ <silk chain> ]
 * This file is part of the silk chain library.
 *
 * The silk chain library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The silk chain library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the silk chain library. If not, see <http://www.gnu.org/licenses/>.
 */
package org.tdf.sunflower.vrf.mine;

import static org.junit.Assert.assertTrue;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.catalina.startup.ClassLoaderFactory.Repository;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;
import org.tdf.common.util.HexBytes;
import org.tdf.crypto.PrivateKey;
import org.tdf.crypto.ed25519.Ed25519PrivateKey;
import org.tdf.sunflower.consensus.vrf.HashUtil;
import org.tdf.sunflower.consensus.vrf.VrfStateMachine;
import org.tdf.sunflower.consensus.vrf.core.BlockIdentifier;
import org.tdf.sunflower.consensus.vrf.core.CommitProof;
import org.tdf.sunflower.consensus.vrf.core.PendingVrfState;
import org.tdf.sunflower.consensus.vrf.core.ProposalProof;
import org.tdf.sunflower.consensus.vrf.core.StateMachineListener;
import org.tdf.sunflower.consensus.vrf.core.Validator;
import org.tdf.sunflower.consensus.vrf.core.ValidatorManager;
import org.tdf.sunflower.consensus.vrf.core.VrfBlockWrapper;
import org.tdf.sunflower.consensus.vrf.core.VrfProof;
import org.tdf.sunflower.consensus.vrf.core.VrfRound;
import org.tdf.sunflower.consensus.vrf.db.HashMapDB;
import org.tdf.sunflower.consensus.vrf.struct.VrfPrivateKey;
import org.tdf.sunflower.consensus.vrf.struct.VrfResult;
import org.tdf.sunflower.consensus.vrf.util.VrfUtil;
import org.tdf.sunflower.facade.BlockRepository;
import org.tdf.sunflower.service.ConsortiumRepositoryService;
import org.tdf.sunflower.types.Header;

/**
 * @author James Hu
 * @since 2019/6/20
 */
public class VrfStateMachineTest {

    private static final byte[] vrfSk0 = Hex.decode("9e72bcb8c7cfff542030f3a56b78581e13f983f994d95d60b7fe4af679bb8cb7");
    private static final byte[] vrfSk1 = Hex.decode("09bb524717b97f0ea5684962ccea964216483157a8170070927bd01c6913d823");
    private static final byte[] vrfSk2 = Hex.decode("489bc5beb122339ea47c06ed83d10b0c266816f3466af2fb319ac0a10abf3017");
    private static final byte[] vrfSk3 = Hex.decode("b2f105f0eccec7a86d6fd9bef1b5d5b858e01f9732a6b1c1b6c09e99f0f25d13");
    private static final byte[] vrfSk4 = Hex.decode("f041db78a30efc5eba35d654adf090254a76d1f64bf5a1d4821e5cf733a5be7b");
    private static final byte[] vrfSk5 = Hex.decode("b8cf34ad4909091b2c1dde1f5cf8b0e49cd32ef5f25fa2f3f6e424d861a07c41");
    private static final byte[] vrfSk6 = Hex.decode("23a5596fe1890f11d9fd008582fdef3618567e2743b6b7099cbeadcb58267d4e");
    private static final byte[] vrfSk7 = Hex.decode("805c4d92e354eefcf90a2b40f944369eac5eda2d055c6f766a93fa4d19e6f6d2");
    private static final byte[] vrfSk8 = Hex.decode("afd94ef2cc78d1fafbdc5b05e395349403168492a32e519872483b91f84a4a8a");
    private static final byte[] vrfSk9 = Hex.decode("c9e7bc8db3f5e13fd78e5f570f48cf2f5421a11af7bb3a40e2533f774f9f29c1");

    private static final byte[][] VRF_SK_ARRAY = new byte[][] {
            vrfSk0, vrfSk1, vrfSk2, vrfSk3, vrfSk4,
            vrfSk5, vrfSk6, vrfSk7, vrfSk8, vrfSk9
    };

    private static final byte[] coinbase0 = Hex.decode("3a0b32b4e6f404934d098957d200e803239fdf75");
    private static final byte[] coinbase1 = Hex.decode("CD2A3D9F938E13CD947EC05ABC7FE734DF8DD826");
    private static final byte[] coinbase2 = Hex.decode("13978AEE95F38490E9769C39B2773ED763D9CD5F");
    private static final byte[] coinbase3 = Hex.decode("DE1E758511A7C67E7DB93D1C23C1060A21DB4615");
    private static final byte[] coinbase4 = Hex.decode("27DC8DE9E9A1CB673543BD5FCE89E83AF09E228F");
    private static final byte[] coinbase5 = Hex.decode("D64A66C28A6AE5150AF5E7C34696502793B91AE7");
    private static final byte[] coinbase6 = Hex.decode("7da1291da8b73c42c9f6307b05e03b8a2663e503");
    private static final byte[] coinbase7 = Hex.decode("6eed625f205171dde6192336dd4b5f59276742fb");
    private static final byte[] coinbase8 = Hex.decode("003d9f0d826e357fea013b37b0f988a1a87e03f0");
    private static final byte[] coinbase9 = Hex.decode("3551b14081dc0fd629671114f49d332f059e0cba");

    private static final byte[][] COINBASE_ARRAY = new byte[][] {
            coinbase0, coinbase1, coinbase2, coinbase3, coinbase4,
            coinbase5, coinbase6, coinbase7, coinbase8, coinbase9
    };

    /* Now we use Ether units as deposit in the unit test */
    private static final long[] DEPOSIT_ARRAY = new long[] {
            Validator.DEPOSIT_MIN_VALUE + 20,
            Validator.DEPOSIT_MIN_VALUE + 690,
            Validator.DEPOSIT_MIN_VALUE + 9327,
            Validator.DEPOSIT_MIN_VALUE + 240,
            Validator.DEPOSIT_MIN_VALUE + 13980,
            Validator.DEPOSIT_MIN_VALUE + 294,
            Validator.DEPOSIT_MIN_VALUE + 598,
            Validator.DEPOSIT_MIN_VALUE + 3958,
            Validator.DEPOSIT_MIN_VALUE + 21398,
            Validator.DEPOSIT_MIN_VALUE + 49899
    };

    private static final int CONSENSUS_REACH_LOOP = 3;
    private static final int CONSENSUS_TRIED_COUNT = 10;

    // Alloc all consensus nodes
    private static ConsensusNode[] consensusNodes = new ConsensusNode[COINBASE_ARRAY.length];

    private void setupNodes(int nodesCount) {
        assertTrue(COINBASE_ARRAY.length >= nodesCount);
        // Setup all consensus nodes
        for (int i = 0; i < nodesCount; ++i) {
            consensusNodes[i] = new ConsensusNode(i, nodesCount);
        }
    }

    private static void cleanupNodes(int nodesCount) {
        assertTrue(COINBASE_ARRAY.length >= nodesCount);
        for (int i = 0; i < nodesCount; ++i) {
            consensusNodes[i].close();
        }
    }

    @Test
    public void testTimerTask() {
        assertTrue(VRF_SK_ARRAY.length == COINBASE_ARRAY.length);
        assertTrue(COINBASE_ARRAY.length == DEPOSIT_ARRAY.length);

	    BlockRepository repository = ConsortiumRepositoryService.NONE;
//	    Repository track = repository.startTracking();

	    // Prepare blockchain repository and validators
	    Validator[] validators = new Validator[COINBASE_ARRAY.length];
	    for (int i = 0; i < COINBASE_ARRAY.length; ++i) {
            BigInteger balance = BigInteger.valueOf(DEPOSIT_ARRAY[i]).multiply(BigInteger.valueOf(10).pow(Validator.ETHER_POW_WEI));
//            track.addBalance(COINBASE_ARRAY[i], balance);

            PrivateKey privateKey = new Ed25519PrivateKey(VRF_SK_ARRAY[i]);
            VrfPrivateKey sk = new VrfPrivateKey(privateKey);
            byte[] vrfPk = sk.generatePublicKey().getEncoded();

            validators[i] = new Validator(COINBASE_ARRAY[i], DEPOSIT_ARRAY[i], vrfPk);
        }

//	    track.commit();

        // Setup Pending Vrf State
        ValidatorManager manager = new ValidatorManager(repository);
        PendingVrfState pendingVrfState = new PendingVrfState(manager);

        // Prepare VRF state machine
        VrfStateMachine vrfStateMachine = new VrfStateMachine(manager, pendingVrfState, null);

        final long stateCycleSeconds = VrfStateMachine.PROPOSAL_TIMEOUT + VrfStateMachine.REDUCTION_TIMEOUT + VrfStateMachine.FINAL_TIMEOUT;
        for (int tried = 0; tried < 10; ++tried) {
            vrfStateMachine.start(tried);

            long delay = 3500 + 1000 * tried;
            try {
                Thread.sleep(delay);
            } catch (java.lang.InterruptedException ex) {

            }

            long stateDuration = delay % stateCycleSeconds;
            if (stateDuration > 0
                    && stateDuration < VrfStateMachine.PROPOSAL_TIMEOUT) {
                assertTrue(vrfStateMachine.getState() == VrfStateMachine.VRF_STATE_PROPOSAL);
            } else if (stateDuration > VrfStateMachine.PROPOSAL_TIMEOUT
                    && stateDuration < (VrfStateMachine.PROPOSAL_TIMEOUT + VrfStateMachine.REDUCTION_TIMEOUT)) {
                assertTrue(vrfStateMachine.getState() == VrfStateMachine.VRF_STATE_REDUCTION);
            } else if (stateDuration > (VrfStateMachine.PROPOSAL_TIMEOUT + VrfStateMachine.REDUCTION_TIMEOUT)
                    && stateDuration < stateCycleSeconds) {
                assertTrue(vrfStateMachine.getState() == VrfStateMachine.VRF_STATE_FINAL);
            }
            VrfRound vrfRound = vrfStateMachine.getVrfRound();
            long round = delay / (VrfStateMachine.PROPOSAL_TIMEOUT + VrfStateMachine.REDUCTION_TIMEOUT + VrfStateMachine.FINAL_TIMEOUT);
            assertTrue(vrfRound.getRound() == round);
            assertTrue(vrfRound.getBlockNum() == tried);
            //System.out.println("--- VRF State Machine: " + vrfStateMachine.getVrfRound());

            vrfStateMachine.stop();
            assertTrue(vrfStateMachine.getState() == VrfStateMachine.VRF_STATE_INIT);
        }

//	    repository.close();
    }

    @Test
    public void testConsensus1() {
        final int NODES_COUNT = 1;
        final byte[] seed = HashUtil.sha3(coinbase0);

        assertTrue(COINBASE_ARRAY.length >= NODES_COUNT);

        setupNodes(NODES_COUNT);

        // Startup all VrfStateMachine to run consensus
        List<Future<VrfStateMachine>> stateMachineFutures = new ArrayList<Future<VrfStateMachine>>();
        for (int nodeIndex = 0; nodeIndex < NODES_COUNT; ++nodeIndex) {
            ConsensusNode node = consensusNodes[nodeIndex];
            VrfStateTask stateTask = new VrfStateTask(node, NODES_COUNT, seed, 10000);
            NodeExecutor executor = new NodeExecutor();
            Future<VrfStateMachine> future = executor.submit(stateTask);
            stateMachineFutures.add(future);
        }

        for (Future<VrfStateMachine> future : stateMachineFutures) {
            try {
                VrfStateMachine vrfStateMachine = future.get();
                assertTrue(vrfStateMachine.getState() == VrfStateMachine.VRF_STATE_INIT);
            } catch (InterruptedException e) {
            } catch (ExecutionException e) {

            }
        }

        cleanupNodes(NODES_COUNT);
    }

    @Test
    public void testConsensus2() {
        final int NODES_COUNT = 2;
        final byte[] seed = HashUtil.sha3(coinbase0);

        assertTrue(COINBASE_ARRAY.length >= NODES_COUNT);

        setupNodes(NODES_COUNT);

        // Startup all VrfStateMachine to run consensus
        List<Future<VrfStateMachine>> stateMachineFutures = new ArrayList<Future<VrfStateMachine>>();
        for (int nodeIndex = 0; nodeIndex < NODES_COUNT; ++nodeIndex) {
            ConsensusNode node = consensusNodes[nodeIndex];
            VrfStateTask stateTask = new VrfStateTask(node, NODES_COUNT, seed, 10000);
            NodeExecutor executor = new NodeExecutor();
            Future<VrfStateMachine> future = executor.submit(stateTask);
            stateMachineFutures.add(future);
        }

        for (Future<VrfStateMachine> future : stateMachineFutures) {
            try {
                VrfStateMachine vrfStateMachine = future.get();
                assertTrue(vrfStateMachine.getState() == VrfStateMachine.VRF_STATE_INIT);
            } catch (InterruptedException e) {
            } catch (ExecutionException e) {

            }
        }

        cleanupNodes(NODES_COUNT);
    }

    @Test
    public void testConsensus3() {
        final int NODES_COUNT = 3;
        final byte[] seed = HashUtil.sha3(coinbase0);

        assertTrue(COINBASE_ARRAY.length >= NODES_COUNT);

        setupNodes(NODES_COUNT);

        // Startup all VrfStateMachine to run consensus
        List<Future<VrfStateMachine>> stateMachineFutures = new ArrayList<Future<VrfStateMachine>>();
        for (int nodeIndex = 0; nodeIndex < NODES_COUNT; ++nodeIndex) {
            ConsensusNode node = consensusNodes[nodeIndex];
            VrfStateTask stateTask = new VrfStateTask(node, NODES_COUNT, seed, 10000);
            NodeExecutor executor = new NodeExecutor();
            Future<VrfStateMachine> future = executor.submit(stateTask);
            stateMachineFutures.add(future);
        }

        for (Future<VrfStateMachine> future : stateMachineFutures) {
            try {
                VrfStateMachine vrfStateMachine = future.get();
                assertTrue(vrfStateMachine.getState() == VrfStateMachine.VRF_STATE_INIT);
            } catch (InterruptedException e) {
            } catch (ExecutionException e) {

            }
        }

        cleanupNodes(NODES_COUNT);
    }

    @Test
    public void testConsensus10() {
        final int NODES_COUNT = COINBASE_ARRAY.length;
        final byte[] seed = HashUtil.sha3(coinbase0);

        assertTrue(VRF_SK_ARRAY.length == COINBASE_ARRAY.length);
        assertTrue(COINBASE_ARRAY.length == DEPOSIT_ARRAY.length);

        setupNodes(NODES_COUNT);

        // Startup all VrfStateMachine to run consensus
        List<Future<VrfStateMachine>> stateMachineFutures = new ArrayList<Future<VrfStateMachine>>();
        for (int nodeIndex = 0; nodeIndex < NODES_COUNT; ++nodeIndex) {
            ConsensusNode node = consensusNodes[nodeIndex];
            VrfStateTask stateTask = new VrfStateTask(node, NODES_COUNT, seed, 10000);
            NodeExecutor executor = new NodeExecutor();
            Future<VrfStateMachine> future = executor.submit(stateTask);
            stateMachineFutures.add(future);
        }

        for (int nodeIndex = 0; nodeIndex < NODES_COUNT; ++nodeIndex) {
            Future<VrfStateMachine> future = stateMachineFutures.get(nodeIndex);

            try {
                VrfStateMachine vrfStateMachine = future.get();
                String timestamp = new SimpleDateFormat("HH:mm:ss.sss").format(new Date());
                System.out.println(timestamp + " future.get VRF State Machine<" + nodeIndex + ">: state " + vrfStateMachine.getState());
                assertTrue(vrfStateMachine.getState() == VrfStateMachine.VRF_STATE_INIT);
            } catch (InterruptedException e) {
            } catch (ExecutionException e) {

            }
        }

        cleanupNodes(NODES_COUNT);
    }

    public class VrfStateTask implements Callable<VrfStateMachine> {
        private final ConsensusNode node;
        private final int nodesCount;
        private final byte[] seed;
        private final int random;
        private final SecureRandom secureRandom;

        private int consensusReached;

        public VrfStateTask(ConsensusNode node, int nodesCount, byte[] seed, int random) {
            this.node = node;
            this.nodesCount = nodesCount;
            this.seed = seed;
            this.random = random;
            secureRandom = new SecureRandom();
        }

        @Override
        public VrfStateMachine call() throws Exception {
            // random delay to start state machine
            long delay = secureRandom.nextInt(random);
            try {
                Thread.sleep(delay);
            } catch (java.lang.InterruptedException ex) {

            }

            VrfStateMachine stateMachine = node.getStateMachine();

            setupConsensus(stateMachine, nodesCount);

            stateMachine.start(0);

            String timestamp = new SimpleDateFormat("HH:mm:ss.sss").format(new Date());
            System.out.println(">>> VRF State Machine<" + node.getNodeIndex() + ">: startup<" + timestamp + ">");

            synchronized (VrfStateTask.this) {
                VrfStateTask.this.wait();
            }
            timestamp = new SimpleDateFormat("HH:mm:ss.sss").format(new Date());
            System.out.println("<<< VRF State Machine<" + node.getNodeIndex() + ">: quit<" + timestamp + ">");

            stateMachine.stop();
            stateMachine.cleanupListeners();

            return stateMachine;
        }

        private boolean broadcast(ConsensusNode otherNode, ProposalProof proof) {
            boolean result = false;
            if (otherNode != node) {
                VrfStateMachine stateMachine = otherNode.getStateMachine();
                if (stateMachine.getState() != VrfStateMachine.VRF_STATE_INIT) {
                    result = stateMachine.addProof(proof, true);
                    if (!result) {
                        String timestamp = new SimpleDateFormat("HH:mm:ss.sss").format(new Date());
                        System.out.println(timestamp + " node<" + otherNode.getNodeIndex() + ">, Fail to add ProposalProof, state:"
                                + stateMachine.getState() + ", " + stateMachine.getVrfRound()
                                + ", from node<" + node.getNodeIndex() + ">");
                    }
                }
            }

            return result;
        }

        private boolean broadcast(ConsensusNode otherNode, CommitProof proof) {
            boolean result = false;
            if (otherNode != node) {
                VrfStateMachine stateMachine = otherNode.getStateMachine();
                if (stateMachine.getState() != VrfStateMachine.VRF_STATE_INIT) {
                    result = stateMachine.addProof(proof, true);
                    if (!result) {
                        String timestamp = new SimpleDateFormat("HH:mm:ss.sss").format(new Date());
                        System.out.println(timestamp + " node<" + otherNode.getNodeIndex() + ">, Fail to add CommitProof, role:"
                                + proof.getVrfProof().getRole() + ", state:"
                                + stateMachine.getState() + ", " + stateMachine.getVrfRound()
                                + ", from node<" + node.getNodeIndex() + ">");
                    }
                }
            }

            return result;
        }

        private void setupConsensus(VrfStateMachine stateMachine, int nodesCount) {
            StateMachineListener listener = new StateMachineListener() {
                @Override
                public void stateChanged(int from, int to) {
                    String timestamp = new SimpleDateFormat("HH:mm:ss.sss").format(new Date());
                    System.out.println(timestamp + " node<" + node.getNodeIndex() + ">, state changed <" + from + "> - <" + to + ">");
                    if (from == VrfStateMachine.VRF_STATE_INIT && to == VrfStateMachine.VRF_STATE_PROPOSAL) {
                        VrfRound vrfRound = stateMachine.getVrfRound();
                        long blockNum = vrfRound.getBlockNum();
                        int round = vrfRound.getRound();
                        ProposalProof proposalProof = node.createProposalProof(blockNum, round, seed);
                        if (proposalProof != null) {
                            // Add proposal to VrfStateMachine
                            stateMachine.addProof(proposalProof, true);

                            // Broadcast proposal to other nodes
                            for (int node = 0; node < nodesCount; ++node) {
                                broadcast(consensusNodes[node], proposalProof);
                            }
                        }
                    } else if (from == VrfStateMachine.VRF_STATE_PROPOSAL && to == VrfStateMachine.VRF_STATE_REDUCTION) {
                        VrfRound vrfRound = stateMachine.getVrfRound();
                        long blockNum = vrfRound.getBlockNum();
                        int round = vrfRound.getRound();

                        ProposalProof bestProproserProof = stateMachine.getBestProposalProof();
                        if (bestProproserProof != null) {
                            assertTrue(bestProproserProof.getBlockIdentifier().getNumber() == blockNum);

                            BlockIdentifier blockIdentifier = bestProproserProof.getBlockIdentifier();
                            CommitProof commitProof = node.createProposalCommitProof(round, seed, blockIdentifier);
                            if (commitProof != null) {
                                // Add commit to VrfStateMachine
                                stateMachine.addProof(commitProof, true);

                                // Broadcast commit to other nodes
                                for (int node = 0; node < nodesCount; ++node) {
                                    broadcast(consensusNodes[node], commitProof);
                                }
                            }
                        }
                    } else if (from == VrfStateMachine.VRF_STATE_REDUCTION && to == VrfStateMachine.VRF_STATE_FINAL) {
                        VrfRound vrfRound = stateMachine.getVrfRound();
                        long blockNum = vrfRound.getBlockNum();
                        int round = vrfRound.getRound();

                        ProposalProof bestProproserProof = stateMachine.getBestProposalProof();
                        BlockIdentifier reachCommitIdentifier = stateMachine.reachCommitBlockIdentifier();
                        if (reachCommitIdentifier == null
                                || !Arrays.equals(bestProproserProof.getBlockIdentifier().getHash(), reachCommitIdentifier.getHash())
                                || bestProproserProof.getBlockIdentifier().getNumber() != reachCommitIdentifier.getNumber()) {
                            timestamp = new SimpleDateFormat("HH:mm:ss.sss").format(new Date());
                            System.out.println(timestamp + " node<" + node.getNodeIndex() + ">, NOT reach reduction commit agreement, give up Final Commit Proof");
                        } else {
                            // Submit Final Commit
                            CommitProof finalProof = node.createFinalCommitProof(round, seed, reachCommitIdentifier);
                            if (finalProof != null) {
                                // Add final to VrfStateMachine
                                stateMachine.addProof(finalProof, true);

                                // Broadcast final to other nodes
                                for (int node = 0; node < nodesCount; ++node) {
                                    broadcast(consensusNodes[node], finalProof);
                                }
                            }
                        }
                    } else if (from == VrfStateMachine.VRF_STATE_FINAL && to == VrfStateMachine.VRF_STATE_PROPOSAL) {
                        VrfRound vrfRound = stateMachine.getVrfRound();
                        long blockNum = vrfRound.getBlockNum();
                        int round = vrfRound.getRound();

                        timestamp = new SimpleDateFormat("HH:mm:ss.sss").format(new Date());
                        System.out.println(timestamp + " node<" + node.getNodeIndex() + ">, new VrfRound<" + blockNum + ", " + round + ">");
                        if (round == 0) {
                            consensusReached++;
                            System.out.println("!!! Reach agreement, <node index>:" + node.getNodeIndex() + ", <time>:" + consensusReached);
                        } else {
                            if (round > CONSENSUS_TRIED_COUNT) {
                                System.out.println("Already Reach agreement, <node index>:" + node.getNodeIndex() + ", <time>:" + consensusReached);
                                System.out.println("Choose to Quit, VRF Round: " + vrfRound);
                                synchronized (VrfStateTask.this) {
                                    VrfStateTask.this.notifyAll();
                                }
                            }
                        }

                        if (consensusReached < CONSENSUS_REACH_LOOP) {
                            ProposalProof proposalProof = node.createProposalProof(blockNum, round, seed);
                            if (proposalProof != null) {
                                // Add proposal to VrfStateMachine
                                stateMachine.addProof(proposalProof, true);

                                // Broadcast proposal to other nodes
                                for (int node = 0; node < nodesCount; ++node) {
                                    broadcast(consensusNodes[node], proposalProof);
                                }
                            }
                        } else {
                            synchronized (VrfStateTask.this) {
                                VrfStateTask.this.notifyAll();
                            }
                        }
                    } else {
                        timestamp = new SimpleDateFormat("HH:mm:ss.sss").format(new Date());
                        System.out.println(timestamp + " node<" + node.getNodeIndex() + ">, ? State Align ?");
                    }
                }

                @Override
                public boolean finalizeNewBlock(VrfRound vrfRound, VrfBlockWrapper finalBlock) {
                    return true;
                }
            };
            stateMachine.addListener(listener);
        }
    }

    public static class ConsensusNode {
        private final int nodeIndex;

        private final int nodesCount;
        private final BlockRepository repository;
        private final ValidatorManager validatorManager;
        private final Header blockHeader;

        private final PendingVrfState pendingVrfState;
        private final VrfStateMachine vrfStateMachine;

        public ConsensusNode(int nodeIndex, int nodesCount) {
            this.nodeIndex = nodeIndex;

            this.nodesCount = nodesCount;

            repository = ConsortiumRepositoryService.NONE;
            validatorManager = new ValidatorManager(repository);
            blockHeader = createBlockHeader();

            setupValidators();

            pendingVrfState = new PendingVrfState(validatorManager);

            // Prepare VRF state machine
            vrfStateMachine = new VrfStateMachine(validatorManager, pendingVrfState, null);
        }

        private Header createBlockHeader() {
            byte [] emptyArray = new byte[0];
            byte [] recentHash = emptyArray;
            // Compose new nonce with coinbase and index, then initialize the new block header
            byte[] nonce = new byte[COINBASE_ARRAY[nodeIndex].length + 4];
            System.arraycopy(COINBASE_ARRAY[nodeIndex], 0, nonce, 0, COINBASE_ARRAY[nodeIndex].length);
            nonce[COINBASE_ARRAY[nodeIndex].length] = (byte)((nodeIndex >> 24) & 0xFF);
            nonce[COINBASE_ARRAY[nodeIndex].length + 1] = (byte) ((nodeIndex >> 16) & 0xFF);
            nonce[COINBASE_ARRAY[nodeIndex].length + 2] = (byte) ((nodeIndex >> 8) & 0xFF);
            nonce[COINBASE_ARRAY[nodeIndex].length + 3] = (byte) (nodeIndex & 0xFF);
            long time = System.currentTimeMillis() / 1000;
            
            Header blockHeader = Header.builder().hashPrev(HexBytes.fromBytes(recentHash))
                    .createdAt(System.currentTimeMillis()).version(1).height(nodeIndex).build();
            VrfUtil.setNonce(blockHeader, nonce);
            VrfUtil.setDifficulty(blockHeader, emptyArray);
            VrfUtil.setMiner(blockHeader, COINBASE_ARRAY[nodeIndex]);

            return blockHeader;
        }

        private void setupValidators() {
            assertTrue(VRF_SK_ARRAY.length == COINBASE_ARRAY.length);
            assertTrue(COINBASE_ARRAY.length == DEPOSIT_ARRAY.length);
            assertTrue(COINBASE_ARRAY.length >= nodesCount);

//            Repository track = repository.startTracking();
            Validator[] validators = new Validator[nodesCount];
            // Add up all weights to validator manager
            for (int i = 0; i < nodesCount; ++i) {
                BigInteger balance = BigInteger.valueOf(DEPOSIT_ARRAY[i]).multiply(BigInteger.valueOf(10).pow(Validator.ETHER_POW_WEI));
//                track.addBalance(COINBASE_ARRAY[i], balance);

                PrivateKey privateKey = new Ed25519PrivateKey(VRF_SK_ARRAY[i]);
                VrfPrivateKey sk = new VrfPrivateKey(privateKey);
                byte[] vrfPk = sk.generatePublicKey().getEncoded();

                validators[i] = new Validator(COINBASE_ARRAY[i], DEPOSIT_ARRAY[i], vrfPk);
            }

//            track.commit();
        }

        public int getNodeIndex() {
            return nodeIndex;
        }

        public VrfStateMachine getStateMachine() {
            return vrfStateMachine;
        }

        public ProposalProof createProposalProof(long blockNum, int round, byte[] seed) {
            PrivateKey privateKey = new Ed25519PrivateKey(VRF_SK_ARRAY[nodeIndex]);
            VrfPrivateKey sk = new VrfPrivateKey(privateKey);

            byte[] vrfPk = sk.generatePublicKey().getEncoded();

            // Must use VrfProof Util to proof with Role Code
            VrfResult vrfResult = VrfProof.Util.prove(VrfProof.ROLE_CODES_PROPOSER, round, sk, seed);
            VrfProof vrfProof = VrfProof.Util.vrfProof(VrfProof.ROLE_CODES_PROPOSER, round, vrfPk, seed, vrfResult);

            // Update block number
            byte[] identifier = blockHeader.getHash().getBytes();

            BlockIdentifier blockIdentifier = new BlockIdentifier(identifier, blockNum);
            ProposalProof proof = new ProposalProof(vrfProof, COINBASE_ARRAY[nodeIndex], blockIdentifier, sk.getSigner());

            // Check priority of new proof
            int priority = validatorManager.getPriority(proof, ValidatorManager.EXPECTED_PROPOSER_THRESHOLD);
            if (priority > 0) {
                return proof;
            }

            return null;
        }

        public CommitProof createProposalCommitProof(int round, byte[] seed, BlockIdentifier blockIdentifier) {
            PrivateKey privateKey = new Ed25519PrivateKey(VRF_SK_ARRAY[nodeIndex]);
            VrfPrivateKey sk = new VrfPrivateKey(privateKey);

            byte[] vrfPk = sk.generatePublicKey().getEncoded();

            // Must use VrfProof Util to proof with Role Code
            VrfResult vrfResult = VrfProof.Util.prove(VrfProof.ROLE_CODES_REDUCTION_COMMIT, round, sk, seed);
            VrfProof vrfProof = VrfProof.Util.vrfProof(VrfProof.ROLE_CODES_REDUCTION_COMMIT, round, vrfPk, seed, vrfResult);

            CommitProof proof = new CommitProof(vrfProof, COINBASE_ARRAY[nodeIndex], blockIdentifier, sk.getSigner());

            // Check priority of new proof
            int priority = validatorManager.getPriority(proof, ValidatorManager.EXPECTED_PROPOSER_THRESHOLD);
            if (priority > 0) {
                return proof;
            }

            return null;
        }

        public CommitProof createFinalCommitProof(int round, byte[] seed, BlockIdentifier blockIdentifier) {
            PrivateKey privateKey = new Ed25519PrivateKey(VRF_SK_ARRAY[nodeIndex]);
            VrfPrivateKey sk = new VrfPrivateKey(privateKey);

            byte[] vrfPk = sk.generatePublicKey().getEncoded();

            // Must use VrfProof Util to proof with Role Code
            VrfResult vrfResult = VrfProof.Util.prove(VrfProof.ROLE_CODES_FINAL_COMMIT, round, sk, seed);
            VrfProof vrfProof = VrfProof.Util.vrfProof(VrfProof.ROLE_CODES_FINAL_COMMIT, round, vrfPk, seed, vrfResult);

            CommitProof proof = new CommitProof(vrfProof, COINBASE_ARRAY[nodeIndex], blockIdentifier, sk.getSigner());
            // Check priority of new proof
            int priority = validatorManager.getPriority(proof, ValidatorManager.EXPECTED_PROPOSER_THRESHOLD);
            if (priority > 0) {
                return proof;
            }

            return null;
        }

        public void close() {
//            repository.close();
        }
    }

    public static class NodeExecutor {

        private ExecutorService executor = Executors.newFixedThreadPool(1);

        public Future<VrfStateMachine> submit(VrfStateTask task) {
            return executor.submit(task);
        }
    }
}