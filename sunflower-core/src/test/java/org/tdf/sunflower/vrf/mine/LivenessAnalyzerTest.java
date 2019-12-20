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
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;
import org.tdf.common.util.HexBytes;
import org.tdf.crypto.PrivateKey;
import org.tdf.crypto.ed25519.Ed25519PrivateKey;
import org.tdf.sunflower.consensus.vrf.HashUtil;
import org.tdf.sunflower.consensus.vrf.contract.PrecompiledContracts;
import org.tdf.sunflower.consensus.vrf.contract.VrfContracts;
import org.tdf.sunflower.consensus.vrf.core.BlockIdentifier;
import org.tdf.sunflower.consensus.vrf.core.CommitProof;
import org.tdf.sunflower.consensus.vrf.core.Liveness;
import org.tdf.sunflower.consensus.vrf.core.LivenessAnalyzer;
import org.tdf.sunflower.consensus.vrf.core.PendingVrfState;
import org.tdf.sunflower.consensus.vrf.core.ProofValidationResult;
import org.tdf.sunflower.consensus.vrf.core.ProposalProof;
import org.tdf.sunflower.consensus.vrf.core.Validator;
import org.tdf.sunflower.consensus.vrf.core.ValidatorManager;
import org.tdf.sunflower.consensus.vrf.core.VrfProof;
import org.tdf.sunflower.consensus.vrf.core.VrfRound;
import org.tdf.sunflower.consensus.vrf.struct.VrfPrivateKey;
import org.tdf.sunflower.consensus.vrf.struct.VrfResult;
import org.tdf.sunflower.consensus.vrf.util.FastByteComparisons;
import org.tdf.sunflower.consensus.vrf.util.VrfUtil;
import org.tdf.sunflower.consensus.vrf.vm.DataWord;
import org.tdf.sunflower.facade.BlockRepository;
import org.tdf.sunflower.service.ConsortiumRepositoryService;
import org.tdf.sunflower.types.Header;
import org.tdf.sunflower.util.ByteUtil;

/**
 * @author James Hu
 * @since 2019/07/15
 */
public class LivenessAnalyzerTest {

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

    private static final byte[][] VRF_SK_ARRAY = new byte[][] { vrfSk0, vrfSk1, vrfSk2, vrfSk3, vrfSk4, vrfSk5, vrfSk6,
            vrfSk7, vrfSk8, vrfSk9 };

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

    private static final byte[][] COINBASE_ARRAY = new byte[][] { coinbase0, coinbase1, coinbase2, coinbase3, coinbase4,
            coinbase5, coinbase6, coinbase7, coinbase8, coinbase9 };

    /* Now we use Ether units as weight in the unit test */
    private static final long[] WEIGHT_ARRAY = new long[] {
            /* IMPORTANT: Set first one deposit < DEPOSIT_THRESHOLD_AS_VALIDATOR */
            20, Validator.DEPOSIT_MIN_VALUE + Validator.DEPOSIT_UNIT_VALUE, Validator.DEPOSIT_MIN_VALUE,
            Validator.DEPOSIT_MIN_VALUE, Validator.DEPOSIT_MIN_VALUE, Validator.DEPOSIT_MIN_VALUE,
            Validator.DEPOSIT_MIN_VALUE, Validator.DEPOSIT_MIN_VALUE, Validator.DEPOSIT_MIN_VALUE,
            Validator.DEPOSIT_MIN_VALUE + Validator.DEPOSIT_UNIT_VALUE + Validator.DEPOSIT_UNIT_VALUE };

    private static final int LATEST_HIT_INDEX = 1000;
    private static final long[][] ACTIVATE_ARRAY = new long[][] { { 5, 1 }, { 38, 1 }, { 79, 1 }, { 113, 1 },
            { 138, 2 }, { 173, 1 }, { 211, 1 }, { 367, 1 }, { 531, 2 }, { LATEST_HIT_INDEX, 3 }, };

    private static final BigInteger DEPOSIT_THRESHOLD = BigInteger.valueOf(Validator.DEPOSIT_MIN_VALUE)
            .multiply(Validator.ETHER_TO_WEI);
    private static final BigInteger DEPOSIT_UNIT_WEI = BigInteger.valueOf(Validator.DEPOSIT_UNIT_VALUE)
            .multiply(Validator.ETHER_TO_WEI);
    /* Now we use Wei units as deposit in the unit test */
    private static final BigInteger[] DEPOSIT_ARRAY = new BigInteger[] {
            BigInteger.valueOf(20).multiply(Validator.ETHER_TO_WEI), DEPOSIT_THRESHOLD.add(DEPOSIT_UNIT_WEI),
            DEPOSIT_THRESHOLD, DEPOSIT_THRESHOLD, DEPOSIT_THRESHOLD, DEPOSIT_THRESHOLD, DEPOSIT_THRESHOLD,
            DEPOSIT_THRESHOLD, DEPOSIT_THRESHOLD, DEPOSIT_THRESHOLD.add(DEPOSIT_UNIT_WEI).add(DEPOSIT_UNIT_WEI) };

    private static final DataWord contractAddr = DataWord
            .of("0000000000000000000000000000000000000000000000000000000000000011");

    private static final BlockRepository repository = ConsortiumRepositoryService.NONE;
//    private static final Repository cacheTrack = repository.startTracking();

    private static Header[] blockHeaders;

    private ProposalProof createProposalProof(int index, long blockNum, int round, byte[] seed) {
        PrivateKey privateKey = new Ed25519PrivateKey(VRF_SK_ARRAY[index]);
        VrfPrivateKey sk = new VrfPrivateKey(privateKey);

        byte[] vrfPk = sk.generatePublicKey().getEncoded();

        // Must use VrfProof Util to proof with Role Code
        VrfResult vrfResult = VrfProof.Util.prove(VrfProof.ROLE_CODES_PROPOSER, round, sk, seed);
        VrfProof vrfProof = VrfProof.Util.vrfProof(VrfProof.ROLE_CODES_PROPOSER, round, vrfPk, seed, vrfResult);

        // Update block number
        blockHeaders[index].setHeight(blockNum);
        byte[] identifier = blockHeaders[index].getHash().getBytes();

        BlockIdentifier blockIdentifier = new BlockIdentifier(identifier, blockNum);
        ProposalProof proof = new ProposalProof(vrfProof, COINBASE_ARRAY[index], blockIdentifier, sk.getSigner());

        return proof;
    }

    private CommitProof createReductionCommitProof(int index, long blockNum, int round, byte[] seed,
            ProposalProof proposalProof) {
        PrivateKey privateKey = new Ed25519PrivateKey(VRF_SK_ARRAY[index]);
        VrfPrivateKey sk = new VrfPrivateKey(privateKey);

        byte[] vrfPk = sk.generatePublicKey().getEncoded();

        // Must use VrfProof Util to proof with Role Code
        VrfResult vrfResult = VrfProof.Util.prove(VrfProof.ROLE_CODES_REDUCTION_COMMIT, round, sk, seed);
        VrfProof vrfProof = VrfProof.Util.vrfProof(VrfProof.ROLE_CODES_REDUCTION_COMMIT, round, vrfPk, seed, vrfResult);

        BlockIdentifier blockIdentifier = proposalProof.getBlockIdentifier();
        CommitProof proof = new CommitProof(vrfProof, COINBASE_ARRAY[index], blockIdentifier, sk.getSigner());

        return proof;
    }

    private CommitProof createFinalCommitProof(int index, long blockNum, int round, byte[] seed,
            ProposalProof proposalProof) {
        PrivateKey privateKey = new Ed25519PrivateKey(VRF_SK_ARRAY[index]);
        VrfPrivateKey sk = new VrfPrivateKey(privateKey);

        byte[] vrfPk = sk.generatePublicKey().getEncoded();

        // Must use VrfProof Util to proof with Role Code
        VrfResult vrfResult = VrfProof.Util.prove(VrfProof.ROLE_CODES_FINAL_COMMIT, round, sk, seed);
        VrfProof vrfProof = VrfProof.Util.vrfProof(VrfProof.ROLE_CODES_FINAL_COMMIT, round, vrfPk, seed, vrfResult);

        BlockIdentifier blockIdentifier = proposalProof.getBlockIdentifier();
        CommitProof proof = new CommitProof(vrfProof, COINBASE_ARRAY[index], blockIdentifier, sk.getSigner());

        return proof;
    }

    private static void createBlockHeaders() {
        blockHeaders = new Header[COINBASE_ARRAY.length];

        for (int i = 0; i < COINBASE_ARRAY.length; ++i) {
            byte[] emptyArray = new byte[0];
            byte[] recentHash = emptyArray;
            // Compose new nonce with coinbase and index and initialize new block header
            byte[] nonce = new byte[COINBASE_ARRAY[i].length + 4];
            System.arraycopy(COINBASE_ARRAY[i], 0, nonce, 0, COINBASE_ARRAY[i].length);
            nonce[COINBASE_ARRAY[i].length] = (byte) ((i >> 24) & 0xFF);
            nonce[COINBASE_ARRAY[i].length + 1] = (byte) ((i >> 16) & 0xFF);
            nonce[COINBASE_ARRAY[i].length + 2] = (byte) ((i >> 8) & 0xFF);
            nonce[COINBASE_ARRAY[i].length + 3] = (byte) (i & 0xFF);
            long time = System.currentTimeMillis() / 1000;

            blockHeaders[i] = Header.builder().hashPrev(HexBytes.fromBytes(recentHash))
                    .createdAt(System.currentTimeMillis()).version(1).height(i).build();
            VrfUtil.setNonce(blockHeaders[i], nonce);
            VrfUtil.setDifficulty(blockHeaders[i], emptyArray);
            VrfUtil.setMiner(blockHeaders[i], COINBASE_ARRAY[i]);
        }
    }

    @BeforeClass
    public static void setup() {
        assertTrue(COINBASE_ARRAY.length == DEPOSIT_ARRAY.length);

        createBlockHeaders();

        // Setup Validator Registration for all coinbases
        final String pubkey = new String("bbb361229922ff28bd992c5bbe344c7afdef76fd18c796618c9e9ab7ebae65d9");
        for (int i = 0; i < COINBASE_ARRAY.length; ++i) {
            BigInteger deposit = DEPOSIT_ARRAY[i];

            PrecompiledContracts.PrecompiledContract contract = VrfContracts.getContractForAddress(contractAddr,
                    COINBASE_ARRAY[i], deposit.toByteArray(), repository, 0);
            // Call depositPubkey
            byte[] data = Hex.decode("554f40e0" + pubkey);
            Pair<Boolean, byte[]> result = contract.execute(data);
            if (deposit.compareTo(DEPOSIT_THRESHOLD) < 0) {
                assertTrue(result.getLeft() == false);
            } else {
                assertTrue(result.getLeft() == true);
            }

//            cacheTrack.addBalance(contractAddr.getLast20Bytes(), deposit);
        }

        PrecompiledContracts.PrecompiledContract contract = VrfContracts.getContractForAddress(contractAddr,
                COINBASE_ARRAY[0], null, repository, 0);
        // Call getValidSize
        byte[] data = Hex.decode("4c375f7d");
        Pair<Boolean, byte[]> result = contract.execute(data);
        assertTrue(result.getLeft() == true);

        int validVds = 0;
        BigInteger validSize = BigInteger.ZERO;
        for (int i = 0; i < DEPOSIT_ARRAY.length; ++i) {
            BigInteger deposit = DEPOSIT_ARRAY[i];
            if (deposit.compareTo(DEPOSIT_THRESHOLD) >= 0) {
                validSize = validSize.add(DEPOSIT_ARRAY[i]);
                validVds++;
            }
        }
        assertTrue(ByteUtil.bytesToBigInteger(result.getRight()).compareTo(validSize) == 0);

        // Call getValidVds
        data = Hex.decode("f3c07c9f");
        result = contract.execute(data);
        assertTrue(result.getLeft() == true);

        assertTrue(ByteUtil.byteArrayToInt(result.getRight()) == validVds);
    }

    @AfterClass
    public static void cleanup() {
        assertTrue(0 == 0);
    }

    @Test
    public void testOverflow() {
        long param1 = 18364;
        long param2 = 92476553;

        long start = Long.MAX_VALUE - param1;
        long stop = start + param2;

        if (stop < 0) {
            stop = Long.MAX_VALUE + 1 + stop;
        }

        assertTrue(stop == (param2 - param1 - 1));

        start = -1 - Long.MAX_VALUE;
        stop = Long.MIN_VALUE;
        assertTrue(start == stop);
    }

    @Test
    public void testPut() {

        assertTrue(VRF_SK_ARRAY.length == COINBASE_ARRAY.length);
        assertTrue(COINBASE_ARRAY.length == WEIGHT_ARRAY.length);

        LivenessAnalyzer lvnAnalyzer = new LivenessAnalyzer();

        Liveness[] lvns = new Liveness[COINBASE_ARRAY.length];
        for (int i = 0; i < COINBASE_ARRAY.length; ++i) {
            lvns[i] = new Liveness(COINBASE_ARRAY[i], ACTIVATE_ARRAY[i][0], ACTIVATE_ARRAY[i][1]);
            lvnAnalyzer.addLiveness(lvns[i]);
        }

        // Try to check liveness in LivenessAnalyzer
        for (int i = 0; i < COINBASE_ARRAY.length; ++i) {
            Liveness lvn = lvnAnalyzer.getLiveness(COINBASE_ARRAY[i]);

            assertTrue(lvns[i] == lvn);
        }
    }

    @Test
    public void testDelete() {

        assertTrue(VRF_SK_ARRAY.length == COINBASE_ARRAY.length);
        assertTrue(COINBASE_ARRAY.length == WEIGHT_ARRAY.length);

        LivenessAnalyzer lvnAnalyzer = new LivenessAnalyzer();

        Liveness[] lvns = new Liveness[COINBASE_ARRAY.length];
        for (int i = 0; i < COINBASE_ARRAY.length; ++i) {
            lvns[i] = new Liveness(COINBASE_ARRAY[i], ACTIVATE_ARRAY[i][0], ACTIVATE_ARRAY[i][1]);
            lvnAnalyzer.addLiveness(lvns[i]);
        }

        // Try to check liveness in LivenessAnalyzer
        for (int i = 0; i < COINBASE_ARRAY.length; ++i) {
            Liveness lvn = lvnAnalyzer.getLiveness(COINBASE_ARRAY[i]);

            assertTrue(lvns[i] == lvn);
        }

        // Try to remove all liveness from LivenessAnalyzer
        for (int i = 0; i < COINBASE_ARRAY.length; ++i) {
            lvnAnalyzer.removeLiveness(lvns[i]);
        }

        // Try to check liveness in LivenessAnalyzer
        for (int i = 0; i < COINBASE_ARRAY.length; ++i) {
            Liveness lvn = lvnAnalyzer.getLiveness(COINBASE_ARRAY[i]);

            assertTrue(null == lvn);
        }
    }

    @Test
    public void testMisses1() {

        assertTrue(VRF_SK_ARRAY.length == COINBASE_ARRAY.length);
        assertTrue(COINBASE_ARRAY.length == WEIGHT_ARRAY.length);

        long totalWeight = 0;

        LivenessAnalyzer lvnAnalyzer = new LivenessAnalyzer();

        Liveness[] lvns = new Liveness[COINBASE_ARRAY.length];
        for (int i = 0; i < COINBASE_ARRAY.length; ++i) {
            lvns[i] = new Liveness(COINBASE_ARRAY[i], ACTIVATE_ARRAY[i][0], ACTIVATE_ARRAY[i][1]);
            lvnAnalyzer.addLiveness(lvns[i]);

            if (WEIGHT_ARRAY[i] >= Validator.DEPOSIT_MIN_VALUE) {
                totalWeight += WEIGHT_ARRAY[i];
            }
        }

        // Try to check liveness in LivenessAnalyzer
        for (int i = 0; i < COINBASE_ARRAY.length; ++i) {
            Liveness lvn = lvnAnalyzer.getLiveness(COINBASE_ARRAY[i]);

            assertTrue(lvns[i] == lvn);
        }

        // Active all liveness whose Deposit >= DEPOSIT_MIN_VALUE
        for (int i = 0; i < COINBASE_ARRAY.length; ++i) {
            // Active validator liveness
            if (WEIGHT_ARRAY[i] >= Validator.DEPOSIT_MIN_VALUE) {
                lvns[i].activate(ACTIVATE_ARRAY[i][0], ACTIVATE_ARRAY[i][1]);
                lvns[i].activate(ACTIVATE_ARRAY[i][0] + 30, 1);
                lvns[i].activate(ACTIVATE_ARRAY[i][0] + 70, ACTIVATE_ARRAY[i][1]);
            }
        }

        // Try to test clean with latest block number set to
        // Liveness.BLOCKS_PER_INACTIVE_EPOCH + 2011 + 10
        for (int i = 0; i < COINBASE_ARRAY.length; ++i) {
            if (WEIGHT_ARRAY[i] >= Validator.DEPOSIT_MIN_VALUE) {
                long misses = lvns[i].stampMisses(LATEST_HIT_INDEX + 100, WEIGHT_ARRAY[i], totalWeight);
                assertTrue(misses == 1);

                // System.out.println("misses{" + i + "} = " + misses);
            }

            if (WEIGHT_ARRAY[i] >= Validator.DEPOSIT_MIN_VALUE) {
                long misses = lvns[i].stampMisses(LATEST_HIT_INDEX + 150, WEIGHT_ARRAY[i], totalWeight);
                assertTrue(misses == 2);

                // System.out.println("misses{" + i + "} = " + misses);
            }

            if (WEIGHT_ARRAY[i] >= Validator.DEPOSIT_MIN_VALUE) {
                long misses = lvns[i].stampMisses(LATEST_HIT_INDEX + 200, WEIGHT_ARRAY[i], totalWeight);
                assertTrue(misses == 3);

                // System.out.println("misses{" + i + "} = " + misses);
            }
        }
    }

    @Test
    public void testMisses2() {

        assertTrue(VRF_SK_ARRAY.length == COINBASE_ARRAY.length);
        assertTrue(COINBASE_ARRAY.length == WEIGHT_ARRAY.length);

        long totalWeight = 0;

        LivenessAnalyzer lvnAnalyzer = new LivenessAnalyzer();

        Liveness[] lvns = new Liveness[COINBASE_ARRAY.length];
        for (int i = 0; i < COINBASE_ARRAY.length; ++i) {
            lvns[i] = new Liveness(COINBASE_ARRAY[i], ACTIVATE_ARRAY[i][0], ACTIVATE_ARRAY[i][1]);
            lvnAnalyzer.addLiveness(lvns[i]);

            if (WEIGHT_ARRAY[i] >= Validator.DEPOSIT_MIN_VALUE) {
                totalWeight += WEIGHT_ARRAY[i];
            }
        }

        // Try to check liveness in LivenessAnalyzer
        for (int i = 0; i < COINBASE_ARRAY.length; ++i) {
            Liveness lvn = lvnAnalyzer.getLiveness(COINBASE_ARRAY[i]);

            assertTrue(lvns[i] == lvn);
        }

        // Active all liveness whose Deposit >= DEPOSIT_MIN_VALUE
        for (int i = 0; i < COINBASE_ARRAY.length; ++i) {
            // Active validator liveness
            if (WEIGHT_ARRAY[i] >= Validator.DEPOSIT_MIN_VALUE) {
                lvns[i].activate(ACTIVATE_ARRAY[i][0], ACTIVATE_ARRAY[i][1]);
                lvns[i].activate(ACTIVATE_ARRAY[i][0] + 30, 1);
                lvns[i].activate(ACTIVATE_ARRAY[i][0] + 70, ACTIVATE_ARRAY[i][1]);
            }
        }

        // Try to test clean with latest block number set to
        // Liveness.BLOCKS_PER_INACTIVE_EPOCH + 2011 + 10
        for (int i = 0; i < COINBASE_ARRAY.length; ++i) {
            if (WEIGHT_ARRAY[i] >= Validator.DEPOSIT_MIN_VALUE) {
                long misses = lvns[i].stampMisses(LATEST_HIT_INDEX + 100, WEIGHT_ARRAY[i], totalWeight);
                assertTrue(misses == 1);

                // System.out.println("misses{" + i + "} = " + misses);
            }

            if (WEIGHT_ARRAY[i] >= Validator.DEPOSIT_MIN_VALUE) {
                long misses = lvns[i].stampMisses(LATEST_HIT_INDEX + 150, WEIGHT_ARRAY[i], totalWeight);
                assertTrue(misses == 2);

                // System.out.println("misses{" + i + "} = " + misses);
            }

            if (WEIGHT_ARRAY[i] >= Validator.DEPOSIT_MIN_VALUE) {
                long misses = lvns[i].stampMisses(LATEST_HIT_INDEX + 200, WEIGHT_ARRAY[i], totalWeight);
                assertTrue(misses == 3);

                // System.out.println("misses{" + i + "} = " + misses);
            }
        }
    }

    @Test
    public void testLiveness1() {

        assertTrue(VRF_SK_ARRAY.length == COINBASE_ARRAY.length);
        assertTrue(COINBASE_ARRAY.length == WEIGHT_ARRAY.length);

        byte[] seed = HashUtil.sha3(coinbase1);
        ValidatorManager validatorManager = new ValidatorManager(repository);
        LivenessAnalyzer lvnAnalyzer = new LivenessAnalyzer();

        PendingVrfState pendingVrfState = new PendingVrfState(validatorManager);

        for (long blockNum = 0; blockNum < 1000; ++blockNum) {
            // Commit to best block header
            Header blockHeader = null;

            for (int tried = 0; tried < 10; ++tried) {
                VrfRound vrfRound = new VrfRound(this);
                vrfRound.setVrfRound(this, blockNum, tried);
                pendingVrfState.setVrfRound(vrfRound);

                // Add all proposer proof to pending proof
                ProposalProof priorityProof = null;
                int bestPriority = 0;
                int proposers = 0;
                int hits = 0;
                int blockIndex = -1;
                for (int i = 0; i < COINBASE_ARRAY.length; ++i) {
                    ProposalProof proposalProof = createProposalProof(i, blockNum, tried, seed);

                    // Check new proof's priority
                    int priority = validatorManager.getPriority(proposalProof,
                            ValidatorManager.EXPECTED_PROPOSER_THRESHOLD);

                    if (priority > 0) {
                        ++proposers;
                        hits += priority;
                        // Do active for liveness one
                        lvnAnalyzer.activate(validatorManager, proposalProof.getCoinbase(), priority);

                        // System.out.println("{" + i + "} priority: " + priority);

                        // Save the best priority and best proposer proof
                        if (priority > bestPriority) {
                            bestPriority = priority;
                            priorityProof = proposalProof;

                            blockHeader = blockHeaders[i];
                            blockIndex = i;
                        } else if (priority == bestPriority) {
                            byte[] newIdentifier = blockHeaders[i].getHash().getBytes();
                            byte[] bestIdentifier = blockHeader.getHash().getBytes();
                            // Compare two block hash as Big Endian order
                            int compare = FastByteComparisons.compareTo(newIdentifier, 0, newIdentifier.length,
                                    bestIdentifier, 0, bestIdentifier.length);
                            if (compare < 0) {
                                bestPriority = priority;
                                priorityProof = proposalProof;

                                blockHeader = blockHeaders[i];
                                blockIndex = i;
                            }
                        }

                        pendingVrfState.addProposalProof(proposalProof);
                    }
                }

                System.out.println("[ proposers: " + proposers + " ], hits: " + hits + " ], blockIndex: " + blockIndex);

                assertTrue(pendingVrfState.getProposalProofSize() == proposers);
                assertTrue(pendingVrfState.getBestPendingProposalProof() == priorityProof);

                // Commit the best proposer proof to pending proof as commit stage
                bestPriority = 0;
                int committers = 0;
                hits = 0;
                for (int i = 0; i < COINBASE_ARRAY.length; ++i) {
                    CommitProof commitProof = createReductionCommitProof(i, blockNum, tried, seed, priorityProof);

                    // Check priority new proof
                    int priority = validatorManager.getPriority(commitProof,
                            ValidatorManager.EXPECTED_PROPOSER_THRESHOLD);

                    if (priority > 0) {
                        ++committers;
                        hits += priority;
                        // Do active for liveness one
                        lvnAnalyzer.activate(validatorManager, commitProof.getCoinbase(), priority);

                        // System.out.println("{" + i + "} priority: " + priority);
                    }

                    // Save the best priority and best proposer proof
                    if (priority > bestPriority) {
                        bestPriority = priority;
                    }

                    pendingVrfState.addCommitProof(commitProof);
                }

                System.out.println("[ committers: " + committers + " ], hits: " + hits);
                assertTrue(pendingVrfState.getProposalCommitSize() == committers);
                if (hits > ValidatorManager.EXPECTED_PROPOSER_THRESHOLD * 2 / 3) {
                    assertTrue(pendingVrfState.validateCommitBlock(blockHeader) == ProofValidationResult.OK);
                } else {
                    continue;
                }

                // Commit the best proposer proof to pending proof as final stage
                bestPriority = 0;
                committers = 0;
                hits = 0;
                for (int i = 0; i < COINBASE_ARRAY.length; ++i) {
                    CommitProof commitProof = createFinalCommitProof(i, blockNum, tried, seed, priorityProof);

                    // Check priority of new proof
                    int priority = validatorManager.getPriority(commitProof,
                            ValidatorManager.EXPECTED_PROPOSER_THRESHOLD);

                    if (priority > 0) {
                        ++committers;
                        hits += priority;
                        // Do active for liveness one
                        lvnAnalyzer.activate(validatorManager, commitProof.getCoinbase(), priority);

                        // System.out.println("{" + i + "} priority: " + priority);
                    }

                    // Save the best priority and best proposer proof
                    if (priority > bestPriority) {
                        bestPriority = priority;
                    }

                    pendingVrfState.addCommitProof(commitProof);
                }

                System.out.println("[ committers: " + committers + " ], hits: " + hits);

                assertTrue(pendingVrfState.getFinalCommitSize() == committers);

                if (hits > ValidatorManager.EXPECTED_PROPOSER_THRESHOLD * 2 / 3) {
                    break;
                }
            }

            assertTrue(pendingVrfState.validateFinalBlock(blockHeader) == ProofValidationResult.OK);
            assertTrue(Arrays.equals(pendingVrfState.reachFinalBlockIdentifier().getHash(),
                    blockHeader.getHash().getBytes()));

            // update seed as block hash
            seed = blockHeader.getHash().getBytes();

            System.out.println("----- [ block number: " + blockNum + " ] -----");
        }

        List<Liveness> list = lvnAnalyzer.getInactiveLiveness(validatorManager);
        assertTrue(list.isEmpty());
    }

    @Test
    public void testLiveness2() {

        assertTrue(VRF_SK_ARRAY.length == COINBASE_ARRAY.length);
        assertTrue(COINBASE_ARRAY.length == WEIGHT_ARRAY.length);

        byte[] seed = HashUtil.sha3(coinbase1);
        ValidatorManager validatorManager = new ValidatorManager(repository);
        LivenessAnalyzer lvnAnalyzer = new LivenessAnalyzer();

        PendingVrfState pendingVrfState = new PendingVrfState(validatorManager);

        final int inactiveIndex = 3;
        for (long blockNum = 0; blockNum < 1000; ++blockNum) {
            // Commit to best block header
            Header blockHeader = null;

            for (int tried = 0; tried < 10; ++tried) {
                VrfRound vrfRound = new VrfRound(this);
                vrfRound.setVrfRound(this, blockNum, tried);
                pendingVrfState.setVrfRound(vrfRound);

                // Add all proposer proof to pending proof
                ProposalProof priorityProof = null;
                int bestPriority = 0;
                int proposers = 0;
                int hits = 0;
                int blockIndex = -1;
                for (int i = 0; i < COINBASE_ARRAY.length; ++i) {
                    if (i == inactiveIndex)
                        continue;

                    ProposalProof proposalProof = createProposalProof(i, blockNum, tried, seed);

                    // Check new proof's priority
                    int priority = validatorManager.getPriority(proposalProof,
                            ValidatorManager.EXPECTED_PROPOSER_THRESHOLD);

                    if (priority > 0) {
                        ++proposers;
                        hits += priority;
                        // Do active for liveness one
                        lvnAnalyzer.activate(validatorManager, proposalProof.getCoinbase(), priority);

                        // System.out.println("{" + i + "} priority: " + priority);

                        // Save the best priority and best proposer proof
                        if (priority > bestPriority) {
                            bestPriority = priority;
                            priorityProof = proposalProof;

                            blockHeader = blockHeaders[i];
                            blockIndex = i;
                        } else if (priority == bestPriority) {
                            byte[] newIdentifier = blockHeaders[i].getHash().getBytes();
                            byte[] bestIdentifier = blockHeader.getHash().getBytes();
                            // Compare two block hash as Big Endian order
                            int compare = FastByteComparisons.compareTo(newIdentifier, 0, newIdentifier.length,
                                    bestIdentifier, 0, bestIdentifier.length);
                            if (compare < 0) {
                                bestPriority = priority;
                                priorityProof = proposalProof;

                                blockHeader = blockHeaders[i];
                                blockIndex = i;
                            }
                        }

                        pendingVrfState.addProposalProof(proposalProof);
                    }
                }

                System.out.println("[ proposers: " + proposers + " ], hits: " + hits + " ], blockIndex: " + blockIndex);

                assertTrue(pendingVrfState.getProposalProofSize() == proposers);
                assertTrue(pendingVrfState.getBestPendingProposalProof() == priorityProof);

                // Commit the best proposer proof to pending proof as commit stage
                bestPriority = 0;
                int committers = 0;
                hits = 0;
                for (int i = 0; i < COINBASE_ARRAY.length; ++i) {
                    if (i == inactiveIndex)
                        continue;

                    CommitProof commitProof = createReductionCommitProof(i, blockNum, tried, seed, priorityProof);

                    // Check priority new proof
                    int priority = validatorManager.getPriority(commitProof,
                            ValidatorManager.EXPECTED_PROPOSER_THRESHOLD);

                    if (priority > 0) {
                        ++committers;
                        hits += priority;
                        // Do active for liveness one
                        lvnAnalyzer.activate(validatorManager, commitProof.getCoinbase(), priority);

                        // System.out.println("{" + i + "} priority: " + priority);
                    }

                    // Save the best priority and best proposer proof
                    if (priority > bestPriority) {
                        bestPriority = priority;
                    }

                    pendingVrfState.addCommitProof(commitProof);
                }

                System.out.println("[ committers: " + committers + " ], hits: " + hits);
                assertTrue(pendingVrfState.getProposalCommitSize() == committers);
                if (hits > ValidatorManager.EXPECTED_PROPOSER_THRESHOLD * 2 / 3) {
                    assertTrue(pendingVrfState.validateCommitBlock(blockHeader) == ProofValidationResult.OK);
                } else {
                    continue;
                }

                // Commit the best proposer proof to pending proof as final stage
                bestPriority = 0;
                committers = 0;
                hits = 0;
                for (int i = 0; i < COINBASE_ARRAY.length; ++i) {
                    if (i == inactiveIndex)
                        continue;

                    CommitProof commitProof = createFinalCommitProof(i, blockNum, tried, seed, priorityProof);

                    // Check priority of new proof
                    int priority = validatorManager.getPriority(commitProof,
                            ValidatorManager.EXPECTED_PROPOSER_THRESHOLD);

                    if (priority > 0) {
                        ++committers;
                        hits += priority;
                        // Do active for liveness one
                        lvnAnalyzer.activate(validatorManager, commitProof.getCoinbase(), priority);

                        // System.out.println("{" + i + "} priority: " + priority);
                    }

                    // Save the best priority and best proposer proof
                    if (priority > bestPriority) {
                        bestPriority = priority;
                    }

                    pendingVrfState.addCommitProof(commitProof);
                }

                System.out.println("[ committers: " + committers + " ], hits: " + hits);

                assertTrue(pendingVrfState.getFinalCommitSize() == committers);

                if (hits > ValidatorManager.EXPECTED_PROPOSER_THRESHOLD * 2 / 3) {
                    break;
                }
            }

            assertTrue(pendingVrfState.validateFinalBlock(blockHeader) == ProofValidationResult.OK);
            assertTrue(Arrays.equals(pendingVrfState.reachFinalBlockIdentifier().getHash(),
                    blockHeader.getHash().getBytes()));

            // update seed as block hash
            seed = blockHeader.getHash().getBytes();

            System.out.println("----- [ block number: " + blockNum + " ] -----");
        }

        List<Liveness> list = lvnAnalyzer.getInactiveLiveness(validatorManager);
        assertTrue(list.size() == 1);
    }
}