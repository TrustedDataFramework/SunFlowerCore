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
package org.tdf.sunflower.vrf.core;

import static org.junit.Assert.assertTrue;

import java.math.BigInteger;
import java.util.Arrays;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.spongycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.tdf.common.util.HexBytes;
import org.tdf.crypto.PrivateKey;
import org.tdf.crypto.ed25519.Ed25519PrivateKey;
import org.tdf.sunflower.TestContext;
import org.tdf.sunflower.consensus.poa.PoAUtils;
import org.tdf.sunflower.consensus.vrf.HashUtil;
import org.tdf.sunflower.consensus.vrf.contract.PrecompiledContracts;
import org.tdf.sunflower.consensus.vrf.contract.VrfContracts;
import org.tdf.sunflower.consensus.vrf.core.BlockIdentifier;
import org.tdf.sunflower.consensus.vrf.core.CommitProof;
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
import org.tdf.sunflower.service.BlockRepositoryService;
import org.tdf.sunflower.types.Header;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestContext.class)
public class PendingVrfStateTest {

    private static final byte[] vrfSk0 = org.spongycastle.util.encoders.Hex
            .decode("9e72bcb8c7cfff542030f3a56b78581e13f983f994d95d60b7fe4af679bb8cb7");
    private static final byte[] vrfSk1 = org.spongycastle.util.encoders.Hex
            .decode("09bb524717b97f0ea5684962ccea964216483157a8170070927bd01c6913d823");
    private static final byte[] vrfSk2 = org.spongycastle.util.encoders.Hex
            .decode("489bc5beb122339ea47c06ed83d10b0c266816f3466af2fb319ac0a10abf3017");
    private static final byte[] vrfSk3 = org.spongycastle.util.encoders.Hex
            .decode("b2f105f0eccec7a86d6fd9bef1b5d5b858e01f9732a6b1c1b6c09e99f0f25d13");
    private static final byte[] vrfSk4 = org.spongycastle.util.encoders.Hex
            .decode("f041db78a30efc5eba35d654adf090254a76d1f64bf5a1d4821e5cf733a5be7b");
    private static final byte[] vrfSk5 = org.spongycastle.util.encoders.Hex
            .decode("b8cf34ad4909091b2c1dde1f5cf8b0e49cd32ef5f25fa2f3f6e424d861a07c41");
    private static final byte[] vrfSk6 = org.spongycastle.util.encoders.Hex
            .decode("23a5596fe1890f11d9fd008582fdef3618567e2743b6b7099cbeadcb58267d4e");
    private static final byte[] vrfSk7 = org.spongycastle.util.encoders.Hex
            .decode("805c4d92e354eefcf90a2b40f944369eac5eda2d055c6f766a93fa4d19e6f6d2");
    private static final byte[] vrfSk8 = org.spongycastle.util.encoders.Hex
            .decode("afd94ef2cc78d1fafbdc5b05e395349403168492a32e519872483b91f84a4a8a");
    private static final byte[] vrfSk9 = org.spongycastle.util.encoders.Hex
            .decode("c9e7bc8db3f5e13fd78e5f570f48cf2f5421a11af7bb3a40e2533f774f9f29c1");

    private static final byte[][] VRF_SK_ARRAY = new byte[][] { vrfSk0, vrfSk1, vrfSk2, vrfSk3, vrfSk4, vrfSk5, vrfSk6,
            vrfSk7, vrfSk8, vrfSk9 };

    private static final byte[] coinbase0 = org.spongycastle.util.encoders.Hex
            .decode("3a0b32b4e6f404934d098957d200e803239fdf75");
    private static final byte[] coinbase1 = org.spongycastle.util.encoders.Hex
            .decode("CD2A3D9F938E13CD947EC05ABC7FE734DF8DD826");
    private static final byte[] coinbase2 = org.spongycastle.util.encoders.Hex
            .decode("13978AEE95F38490E9769C39B2773ED763D9CD5F");
    private static final byte[] coinbase3 = org.spongycastle.util.encoders.Hex
            .decode("DE1E758511A7C67E7DB93D1C23C1060A21DB4615");
    private static final byte[] coinbase4 = org.spongycastle.util.encoders.Hex
            .decode("27DC8DE9E9A1CB673543BD5FCE89E83AF09E228F");
    private static final byte[] coinbase5 = org.spongycastle.util.encoders.Hex
            .decode("D64A66C28A6AE5150AF5E7C34696502793B91AE7");
    private static final byte[] coinbase6 = org.spongycastle.util.encoders.Hex
            .decode("7da1291da8b73c42c9f6307b05e03b8a2663e503");
    private static final byte[] coinbase7 = org.spongycastle.util.encoders.Hex
            .decode("6eed625f205171dde6192336dd4b5f59276742fb");
    private static final byte[] coinbase8 = org.spongycastle.util.encoders.Hex
            .decode("003d9f0d826e357fea013b37b0f988a1a87e03f0");
    private static final byte[] coinbase9 = org.spongycastle.util.encoders.Hex
            .decode("3551b14081dc0fd629671114f49d332f059e0cba");

    private static final byte[][] COINBASE_ARRAY = new byte[][] { coinbase0, coinbase1, coinbase2, coinbase3, coinbase4,
            coinbase5, coinbase6, coinbase7, coinbase8, coinbase9 };

    /* Now we use Ether units as deposit in the unit test */
    private static final long[] DEPOSIT_ARRAY = new long[] {
            /* IMPORTANT: Set first one deposit < DEPOSIT_THRESHOLD_AS_VALIDATOR */
            20, Validator.DEPOSIT_MIN_VALUE + Validator.DEPOSIT_UNIT_VALUE, Validator.DEPOSIT_MIN_VALUE,
            Validator.DEPOSIT_MIN_VALUE + Validator.DEPOSIT_UNIT_VALUE * 2,
            Validator.DEPOSIT_MIN_VALUE + Validator.DEPOSIT_UNIT_VALUE * 4,
            Validator.DEPOSIT_MIN_VALUE + Validator.DEPOSIT_UNIT_VALUE * 7,
            Validator.DEPOSIT_MIN_VALUE + Validator.DEPOSIT_UNIT_VALUE * 5,
            Validator.DEPOSIT_MIN_VALUE + Validator.DEPOSIT_UNIT_VALUE * 8,
            Validator.DEPOSIT_MIN_VALUE + Validator.DEPOSIT_UNIT_VALUE * 6,
            Validator.DEPOSIT_MIN_VALUE + Validator.DEPOSIT_UNIT_VALUE * 3 };

    private static final DataWord contractAddr = DataWord
            .of("0000000000000000000000000000000000000000000000000000000000000011");

    @Autowired
    private BlockRepositoryService repository;
    private ValidatorManager validatorManager;
    private Header[] blockHeaders;

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

    @Before
    public void setup() {
        assertTrue(VRF_SK_ARRAY.length == COINBASE_ARRAY.length);
        assertTrue(COINBASE_ARRAY.length == DEPOSIT_ARRAY.length);

//        repository = ConsortiumRepositoryService.NONE;
//        Repository cacheTrack = repository.startTracking();

        blockHeaders = new Header[COINBASE_ARRAY.length];

        final String pubkey = "bbb361229922ff28bd992c5bbe344c7afdef76fd18c796618c9e9ab7ebae65d9";

        Validator[] validators = new Validator[COINBASE_ARRAY.length];
        for (int i = 0; i < COINBASE_ARRAY.length; ++i) {
            // Setup deposit for all coinbase
            BigInteger deposit = BigInteger.valueOf(DEPOSIT_ARRAY[i]).multiply(Validator.ETHER_TO_WEI);
            PrecompiledContracts.PrecompiledContract contract = VrfContracts.getContractForAddress(contractAddr,
                    COINBASE_ARRAY[i], deposit.toByteArray(), repository, 0);
            // Call depositPubkey
            byte[] data = Hex.decode("554f40e0" + pubkey);
            Pair<Boolean, byte[]> result = contract.execute(data);
            if (i == 0) {
                assertTrue(result.getLeft() == false);
            } else {
                assertTrue(result.getLeft() == true);
            }

//	        cacheTrack.addBalance(contractAddr.getLast20Bytes(), deposit);

            PrivateKey privateKey = new Ed25519PrivateKey(VRF_SK_ARRAY[i]);
            VrfPrivateKey sk = new VrfPrivateKey(privateKey);
            byte[] vrfPk = sk.generatePublicKey().getEncoded();

            validators[i] = new Validator(COINBASE_ARRAY[i], DEPOSIT_ARRAY[i], vrfPk);

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
            blockHeaders[i] = Header.builder().hashPrev(HexBytes.fromBytes(recentHash)).version(1)
                    .createdAt(time).height(i).build();
            VrfUtil.setNonce(blockHeaders[i], nonce);
            VrfUtil.setDifficulty(blockHeaders[i], emptyArray);
            VrfUtil.setMiner(blockHeaders[i], COINBASE_ARRAY[i]);
            blockHeaders[i].setHash(HexBytes.fromBytes(PoAUtils.getHash(blockHeaders[i])));
        }

        // Setup validator manager
        validatorManager = new ValidatorManager(repository);

//	    cacheTrack.commit();
    }

    @AfterClass
    public static void cleanup() {
//        repository.close();
    }

    @Test
    public void testVrfRound() {
        PendingVrfState pendingVrfState = new PendingVrfState(validatorManager);

        VrfRound vrfRound = new VrfRound(this);
        vrfRound.setVrfRound(this, 1000, 0);
        pendingVrfState.setVrfRound(vrfRound);

        assertTrue(pendingVrfState.getVrfRound().getBlockNum() == 1000);
    }

    @Test
    public void testAddProposalProof() {
        final long blockNum = 1000;

        byte[] seed = HashUtil.sha3(coinbase0);

        PendingVrfState pendingVrfState = new PendingVrfState(validatorManager);

        VrfRound vrfRound = new VrfRound(this);
        vrfRound.setVrfRound(this, blockNum, 0);
        pendingVrfState.setVrfRound(vrfRound);

        // Add all proposer proof to pending proof
        ProposalProof priorityProof = null;
        int bestPriority = 0;
        int proposers = 0;
        for (int i = 0; i < COINBASE_ARRAY.length; ++i) {
            ProposalProof proposalProof = createProposalProof(i, blockNum, 0, seed);

            // Check new proof's priority
            int priority = validatorManager.getPriority(proposalProof, ValidatorManager.EXPECTED_PROPOSER_THRESHOLD);

            if (priority > 0) {
                ++proposers;

                System.out.println("{" + i + "} priority: " + priority);
            }

            // Save the best priority and best proposer proof
            if (priority > bestPriority) {
                bestPriority = priority;
                priorityProof = proposalProof;
            }

            pendingVrfState.addProposalProof(proposalProof);
        }

        System.out.println("[ proposers: " + proposers + " ]");

        assertTrue(pendingVrfState.getProposalProofSize() == proposers);
        assertTrue(pendingVrfState.getBestPendingProposalProof() == priorityProof);
    }

    @Test
    public void testAddProposalCommitProof1() {
        final long blockNum = 1000;

        byte[] seed = HashUtil.sha3(coinbase0);

        PendingVrfState pendingVrfState = new PendingVrfState(validatorManager);

        // Commit to best block header
        Header blockHeader = null;

        for (int tried = 0; tried < 3; ++tried) {
            // Reset VRF round
            VrfRound vrfRound = new VrfRound(this);
            vrfRound.setVrfRound(this, blockNum, tried);
            pendingVrfState.setVrfRound(vrfRound);

            // Add all proposer proof to pending proof
            ProposalProof priorityProof = null;
            int bestPriority = 0;
            int proposers = 0;
            int hits = 0;
            for (int i = 0; i < COINBASE_ARRAY.length; ++i) {
                ProposalProof proposalProof = createProposalProof(i, blockNum, tried, seed);

                // Check new proof's priority
                int priority = validatorManager.getPriority(proposalProof,
                        ValidatorManager.EXPECTED_PROPOSER_THRESHOLD);

                if (priority > 0) {
                    ++proposers;
                    hits += priority;

                    System.out.println("{" + i + "} priority: " + priority);
                }

                // Save the best priority and best proposer proof
                if (priority > bestPriority) {
                    bestPriority = priority;
                    priorityProof = proposalProof;

                    blockHeader = blockHeaders[i];
                }

                pendingVrfState.addProposalProof(proposalProof);
            }

            System.out.println("[ proposers: " + proposers + " ], hits: " + hits);

            assertTrue(pendingVrfState.getProposalProofSize() == proposers);
            assertTrue(pendingVrfState.getBestPendingProposalProof() == priorityProof);

            // Commit the best proposer proof to pending proof
            bestPriority = 0;
            int committers = 0;
            hits = 0;
            for (int i = 0; i < COINBASE_ARRAY.length; ++i) {
                CommitProof commitProof = createReductionCommitProof(i, blockNum, tried, seed, priorityProof);

                // Check new proof's priority
                int priority = validatorManager.getPriority(commitProof, ValidatorManager.EXPECTED_PROPOSER_THRESHOLD);

                if (priority > 0) {
                    ++committers;
                    hits += priority;

                    System.out.println("{" + i + "} priority: " + priority);
                }

                // Save the best priority and best proposer proof
                if (priority > bestPriority) {
                    bestPriority = priority;
                }

                pendingVrfState.addCommitProof(commitProof);
            }

            System.out.println("[ committers: " + committers + " ], hits: " + hits);
            assertTrue(pendingVrfState.getProposalCommitSize() == committers);

            if (hits > ValidatorManager.EXPECTED_PROPOSER_THRESHOLD * 2 / 3)
                break;
        }

        assertTrue(pendingVrfState.validateCommitBlock(blockHeader) == ProofValidationResult.OK);
    }

    @Test
    public void testAddProposalCommitProof2() {
        final long blockNum = 1000;

        byte[] seed = HashUtil.sha3(coinbase0);

        PendingVrfState pendingVrfState = new PendingVrfState(validatorManager);

        VrfRound vrfRound = new VrfRound(this);
        vrfRound.setVrfRound(this, blockNum, 0);
        pendingVrfState.setVrfRound(vrfRound);

        // Add all proposer proof to pending proof
        Header blockHeader = null;
        ProposalProof priorityProof = null;
        ProposalProof notGoodProof = null;
        int bestPriority = 0;
        int proposers = 0;
        int hits = 0;
        for (int i = 0; i < COINBASE_ARRAY.length; ++i) {
            ProposalProof proposalProof = createProposalProof(i, blockNum, 0, seed);

            // Check priority of new proof
            int priority = validatorManager.getPriority(proposalProof, ValidatorManager.EXPECTED_PROPOSER_THRESHOLD);

            if (priority > 0) {
                ++proposers;
                hits += priority;

                // Choose the first one as not good proposer proof
                if (notGoodProof == null) {
                    notGoodProof = proposalProof;
                }

                System.out.println("{" + i + "} priority: " + priority);
            }

            // Save the best priority and best proposer proof
            if (priority > bestPriority) {
                bestPriority = priority;
                priorityProof = proposalProof;

                blockHeader = blockHeaders[i];
            }

            pendingVrfState.addProposalProof(proposalProof);
        }

        System.out.println("[ proposers: " + proposers + " ], hits: " + hits);

        assertTrue(pendingVrfState.getProposalProofSize() == proposers);
        assertTrue(pendingVrfState.getBestPendingProposalProof() == priorityProof);

        // Commit the best proposer proof to pending proof
        bestPriority = 0;
        int committers = 0;
        hits = 0;
        for (int i = 0; i < COINBASE_ARRAY.length; ++i) {
            CommitProof commitProof = null;
            // Split two group to commit different proposer proof
            if (i % 2 == 0) {
                commitProof = createReductionCommitProof(i, blockNum, 0, seed, notGoodProof);
            } else {
                commitProof = createReductionCommitProof(i, blockNum, 0, seed, priorityProof);
            }

            // Check new proof's priority
            int priority = validatorManager.getPriority(commitProof, ValidatorManager.EXPECTED_PROPOSER_THRESHOLD);

            if (priority > 0) {
                ++committers;
                hits += priority;

                System.out.println("{" + i + "} priority: " + priority);
            }

            // Save the best priority and best proposer proof
            if (priority > bestPriority) {
                bestPriority = priority;
            }

            pendingVrfState.addCommitProof(commitProof);
        }

        System.out.println("[ committers: " + committers + " ], hits: " + hits);

        assertTrue(pendingVrfState.getProposalCommitSize() == committers);
        assertTrue(pendingVrfState.validateCommitBlock(blockHeader) != ProofValidationResult.OK);
    }

    @Test
    public void testAddProposalCommitProof3() {
        final long blockNum = 1000;

        byte[] seed = HashUtil.sha3(coinbase0);

        PendingVrfState pendingVrfState = new PendingVrfState(validatorManager);

        VrfRound vrfRound = new VrfRound(this);
        vrfRound.setVrfRound(this, blockNum, 0);
        pendingVrfState.setVrfRound(vrfRound);

        // Add all proposer proof to pending proof
        Header blockHeader = null;
        ProposalProof priorityProof = null;
        ProposalProof notGoodProof = null;
        int bestPriority = 0;
        int proposers = 0;
        int hits = 0;
        for (int i = 0; i < COINBASE_ARRAY.length; ++i) {
            ProposalProof proposalProof = createProposalProof(i, blockNum, 0, seed);

            // Check new proof's priority
            int priority = validatorManager.getPriority(proposalProof, ValidatorManager.EXPECTED_PROPOSER_THRESHOLD);

            if (priority > 0) {
                ++proposers;
                hits += priority;

                // Choose the first one as not good proposer proof
                if (notGoodProof == null) {
                    notGoodProof = proposalProof;
                }

                System.out.println("{" + i + "} priority: " + priority);
            }

            // Save the best priority and best proposer proof
            if (priority > bestPriority) {
                bestPriority = priority;
                priorityProof = proposalProof;

                blockHeader = blockHeaders[i];
            }

            pendingVrfState.addProposalProof(proposalProof);
        }

        System.out.println("[ proposers: " + proposers + " ], hits: " + hits);

        assertTrue(pendingVrfState.getProposalProofSize() == proposers);
        assertTrue(pendingVrfState.getBestPendingProposalProof() == priorityProof);

        // Commit the best proposer proof to pending proof
        bestPriority = 0;
        int committers = 0;
        hits = 0;
        for (int i = 0; i < COINBASE_ARRAY.length; ++i) {
            CommitProof commitProof = null;
            // Split two group to commit different proposer proof
            if (i % 2 == 1) {
                commitProof = createReductionCommitProof(i, blockNum, 0, seed, notGoodProof);
            } else {
                commitProof = createReductionCommitProof(i, blockNum, 0, seed, priorityProof);
            }

            // Check new proof's priority
            int priority = validatorManager.getPriority(commitProof, ValidatorManager.EXPECTED_PROPOSER_THRESHOLD);

            if (priority > 0) {
                ++committers;

                System.out.println("{" + i + "} priority: " + priority);
            }

            // Save the best priority and best proposer proof
            if (priority > bestPriority) {
                bestPriority = priority;
            }

            pendingVrfState.addCommitProof(commitProof);
        }

        System.out.println("[ committers: " + committers + " ], hits: " + hits);

        assertTrue(pendingVrfState.getProposalCommitSize() == committers);
        assertTrue(pendingVrfState.validateCommitBlock(blockHeader) != ProofValidationResult.OK);
    }

    @Test
    public void testAddFinalCommitProof1() {
        final long blockNum = 1000;

        byte[] seed = HashUtil.sha3(coinbase0);

        PendingVrfState pendingVrfState = new PendingVrfState(validatorManager);

        // Commit to best block header
        Header blockHeader = null;

        for (int tried = 0; tried < 3; ++tried) {

            VrfRound vrfRound = new VrfRound(this);
            vrfRound.setVrfRound(this, blockNum, tried);
            pendingVrfState.setVrfRound(vrfRound);

            // Add all proposer proof to pending proof
            ProposalProof priorityProof = null;
            int bestPriority = 0;
            int proposers = 0;
            int hits = 0;
            for (int i = 0; i < COINBASE_ARRAY.length; ++i) {
                ProposalProof proposalProof = createProposalProof(i, blockNum, tried, seed);

                // Check new proof's priority
                int priority = validatorManager.getPriority(proposalProof,
                        ValidatorManager.EXPECTED_PROPOSER_THRESHOLD);

                if (priority > 0) {
                    ++proposers;
                    hits += priority;

                    System.out.println("{" + i + "} priority: " + priority);
                }

                // Save the best priority and best proposer proof
                if (priority > bestPriority) {
                    bestPriority = priority;
                    priorityProof = proposalProof;

                    blockHeader = blockHeaders[i];
                }

                pendingVrfState.addProposalProof(proposalProof);
            }

            System.out.println("[ proposers: " + proposers + " ], hits: " + hits);

            assertTrue(pendingVrfState.getProposalProofSize() == proposers);
            assertTrue(pendingVrfState.getBestPendingProposalProof() == priorityProof);

            // Commit the best proposer proof to pending proof
            bestPriority = 0;
            int committers = 0;
            hits = 0;
            for (int i = 0; i < COINBASE_ARRAY.length; ++i) {
                CommitProof commitProof = createFinalCommitProof(i, blockNum, tried, seed, priorityProof);

                // Check new proof's priority
                int priority = validatorManager.getPriority(commitProof, ValidatorManager.EXPECTED_PROPOSER_THRESHOLD);

                if (priority > 0) {
                    ++committers;
                    hits += priority;

                    System.out.println("{" + i + "} priority: " + priority);
                }

                // Save the best priority and best proposer proof
                if (priority > bestPriority) {
                    bestPriority = priority;
                }

                pendingVrfState.addCommitProof(commitProof);
            }

            System.out.println("[ committers: " + committers + " ], hits: " + hits);

            assertTrue(pendingVrfState.getFinalCommitSize() == committers);

            if (hits > ValidatorManager.EXPECTED_PROPOSER_THRESHOLD * 2 / 3)
                break;
        }

        assertTrue(pendingVrfState.validateFinalBlock(blockHeader) == ProofValidationResult.OK);
        assertTrue(
                Arrays.equals(pendingVrfState.reachFinalBlockIdentifier().getHash(), blockHeader.getHash().getBytes()));
    }

    @Test
    public void testAddFinalCommitProof2() {
        final long blockNum = 1000;

        byte[] seed = HashUtil.sha3(coinbase1);

        PendingVrfState pendingVrfState = new PendingVrfState(validatorManager);

        // Commit to best block header
        Header blockHeader = null;

        for (int tried = 0; tried < 3; ++tried) {

            VrfRound vrfRound = new VrfRound(this);
            vrfRound.setVrfRound(this, blockNum, 0);
            pendingVrfState.setVrfRound(vrfRound);

            // Add all proposer proof to pending proof
            ProposalProof priorityProof = null;
            int bestPriority = 0;
            int proposers = 0;
            int hits = 0;
            for (int i = 0; i < COINBASE_ARRAY.length; ++i) {
                ProposalProof proposalProof = createProposalProof(i, blockNum, tried, seed);

                // Check new proof's priority
                int priority = validatorManager.getPriority(proposalProof,
                        ValidatorManager.EXPECTED_PROPOSER_THRESHOLD);

                if (priority > 0) {
                    ++proposers;
                    hits += priority;

                    System.out.println("{" + i + "} priority: " + priority);
                }

                // Save the best priority and best proposer proof
                if (priority > bestPriority) {
                    bestPriority = priority;
                    priorityProof = proposalProof;

                    blockHeader = blockHeaders[i];
                }

                pendingVrfState.addProposalProof(proposalProof);
            }

            System.out.println("[ proposers: " + proposers + " ], hits: " + hits);

            assertTrue(pendingVrfState.getProposalProofSize() == proposers);
            assertTrue(pendingVrfState.getBestPendingProposalProof() == priorityProof);

            // Commit the best proposer proof to pending proof as commit stage
            bestPriority = 0;
            int committers = 0;
            hits = 0;
            for (int i = 0; i < COINBASE_ARRAY.length; ++i) {
                CommitProof commitProof = createReductionCommitProof(i, blockNum, tried, seed, priorityProof);

                // Check new proof's priority
                int priority = validatorManager.getPriority(commitProof, ValidatorManager.EXPECTED_PROPOSER_THRESHOLD);

                if (priority > 0) {
                    ++committers;
                    hits += priority;

                    System.out.println("{" + i + "} priority: " + priority);
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

                // Check new proof's priority
                int priority = validatorManager.getPriority(commitProof, ValidatorManager.EXPECTED_PROPOSER_THRESHOLD);

                if (priority > 0) {
                    ++committers;
                    hits += priority;

                    System.out.println("{" + i + "} priority: " + priority);
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
        assertTrue(
                Arrays.equals(pendingVrfState.reachFinalBlockIdentifier().getHash(), blockHeader.getHash().getBytes()));
    }

    @Test
    public void testLiveness1() {
        byte[] seed = HashUtil.sha3(coinbase1);

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

                        // System.out.println("{" + i + "} priority: " + priority);

                        // Save the best priority and best proposer proof
                        if (priority > bestPriority) {
                            bestPriority = priority;
                            priorityProof = proposalProof;

                            blockHeader = blockHeaders[i];
                            blockIndex = i;
                        } else if (priority == bestPriority) {
                            // Check hash value, and select the small one
                            // Big Endian order
                            byte[] newIdentifier = blockHeaders[i].getHash().getBytes();
                            byte[] bestIdentifier = blockHeader.getHash().getBytes();
                            // Update best one in same block number
                            if (blockHeaders[i].getHeight() == blockHeader.getHeight()) {
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
                        }
                    }

                    pendingVrfState.addProposalProof(proposalProof);
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

            seed = blockHeader.getHash().getBytes();

            System.out.println("----- [ block number: " + blockNum + " ] -----");
        }
    }
}