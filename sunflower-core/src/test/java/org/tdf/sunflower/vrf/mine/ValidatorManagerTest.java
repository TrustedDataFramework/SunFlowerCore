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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.math.BigInteger;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.spongycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.tdf.crypto.PrivateKey;
import org.tdf.crypto.ed25519.Ed25519PrivateKey;
import org.tdf.sunflower.TestContext;
import org.tdf.sunflower.consensus.vrf.HashUtil;
import org.tdf.sunflower.consensus.vrf.contract.PrecompiledContracts;
import org.tdf.sunflower.consensus.vrf.contract.VrfContracts;
import org.tdf.sunflower.consensus.vrf.core.BlockIdentifier;
import org.tdf.sunflower.consensus.vrf.core.ProposalProof;
import org.tdf.sunflower.consensus.vrf.core.Validator;
import org.tdf.sunflower.consensus.vrf.core.ValidatorManager;
import org.tdf.sunflower.consensus.vrf.core.VrfProof;
import org.tdf.sunflower.consensus.vrf.db.ByteArrayWrapper;
import org.tdf.sunflower.consensus.vrf.struct.VrfPrivateKey;
import org.tdf.sunflower.consensus.vrf.struct.VrfResult;
import org.tdf.sunflower.consensus.vrf.vm.DataWord;
import org.tdf.sunflower.facade.BlockRepository;
import org.tdf.sunflower.service.BlockRepositoryService;
import org.tdf.sunflower.util.ByteUtil;

/**
 * @author James Hu
 * @since 2019/5/15
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestContext.class)
@Ignore("not pass")
public class ValidatorManagerTest {

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

    @Autowired
    private BlockRepositoryService repository;
    // private BlockRepository repository; // = ConsortiumRepositoryService.NONE;
//    private static final Repository cacheTrack = repository.startTracking();

    @Before
    public void setup() {
        assertTrue(COINBASE_ARRAY.length == DEPOSIT_ARRAY.length);

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

        BigInteger validSize = BigInteger.ZERO;
        // Skip the first one which is not a valid one in manager because of its
        // balance(20)
        for (int i = 0; i < DEPOSIT_ARRAY.length; ++i) {
            if (DEPOSIT_ARRAY[i].compareTo(DEPOSIT_THRESHOLD) >= 0) {
                validSize = validSize.add(DEPOSIT_ARRAY[i]);
            }
        }
        assertTrue(ByteUtil.bytesToBigInteger(result.getRight()).compareTo(validSize) == 0);

        // Call getValidVds
        data = Hex.decode("f3c07c9f");
        result = contract.execute(data);
        assertTrue(result.getLeft() == true);

        int validVds = 9;
        assertTrue(ByteUtil.byteArrayToInt(result.getRight()) == validVds);

//        cacheTrack.commit();
    }

    @AfterClass
    public static void cleanup() {
        assertTrue(0 == 0);
    }

    @Test
    public void testTotalWeight() {

        assertTrue(VRF_SK_ARRAY.length == COINBASE_ARRAY.length);
        assertTrue(COINBASE_ARRAY.length == WEIGHT_ARRAY.length);

        // Try to test total weight
        long totalWeight = 0;
        ValidatorManager manager = new ValidatorManager(repository);
        // Skip the first one which is not included in manager because of its
        // balance(20)
        for (int i = 0; i < COINBASE_ARRAY.length; ++i) {
            if (WEIGHT_ARRAY[i] >= Validator.DEPOSIT_MIN_VALUE) {
                totalWeight += WEIGHT_ARRAY[i];
            }
        }

        assertTrue(totalWeight / ValidatorManager.WEIGHT_UNIT_OF_DEPOSIT == manager.getTotalWeight());

//        repository.close();
    }

    @Test
    public void testPriorityWeight1() {

        final int expected = 26;

        assertTrue(VRF_SK_ARRAY.length == COINBASE_ARRAY.length);
        assertTrue(COINBASE_ARRAY.length == WEIGHT_ARRAY.length);

        byte[] seed = HashUtil.sha3(COINBASE_ARRAY[0]);

        // Create proves and validators
        ProposalProof[] proves = new ProposalProof[COINBASE_ARRAY.length];
        Validator[] validators = new Validator[COINBASE_ARRAY.length];
        for (int i = 0; i < COINBASE_ARRAY.length; ++i) {
            PrivateKey privateKey = new Ed25519PrivateKey(VRF_SK_ARRAY[i]);
            VrfPrivateKey vrfSk = new VrfPrivateKey(privateKey);
            byte[] vrfPk = vrfSk.generatePublicKey().getEncoded();
            // Must use VrfProve Util to prove with Role Code
            VrfResult vrfResult = VrfProof.Util.prove(VrfProof.ROLE_CODES_PROPOSER, 0, vrfSk, seed);
            VrfProof vrfProof = VrfProof.Util.vrfProof(VrfProof.ROLE_CODES_PROPOSER, 0, vrfPk, seed, vrfResult);

            BlockIdentifier blockIdentifier = new BlockIdentifier(HashUtil.sha3(COINBASE_ARRAY[i]), 1000);
            proves[i] = new ProposalProof(vrfProof, COINBASE_ARRAY[i], blockIdentifier, vrfSk.getSigner());

            validators[i] = new Validator(COINBASE_ARRAY[i], WEIGHT_ARRAY[i], vrfPk);
        }

        // Setup validator manager
        ValidatorManager manager = new ValidatorManager(repository);

        // Test every proposer prove's priority
        final int size = manager.getValidVds();
        assertTrue(size == 9);

        int proposers = 0;
        int proposedWeights = 0;
        int priorities = 0;
        for (int i = 0; i < COINBASE_ARRAY.length; ++i) {
            int priority = manager.getPriority(proves[i], expected);

            System.out.println("priority= " + priority);

            if (priority > 0) {
                ++proposers;
                priorities += priority;
                // proposedWeights += manager.getWeight(proves[i]);
            }
        }

        long totalWeights = manager.getTotalWeight();
        System.out.println("proposers= " + proposers + ", priorities= " + priorities + ", weight= " + proposedWeights
                + ", totalWeights= " + totalWeights);

        assert proposers > 1;
        assert priorities > (expected / 3 * 2);
    }

    @Test
    public void testPriorityWeight2() {

        final int expected = 26;
        long totalWeights = 0;

        assertTrue(VRF_SK_ARRAY.length == COINBASE_ARRAY.length);
        assertTrue(COINBASE_ARRAY.length == DEPOSIT_ARRAY.length);

//        final Repository cacheTrack = repository.startTracking();
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

            // Sum of weight
            if (deposit.compareTo(DEPOSIT_THRESHOLD) >= 0) {
                totalWeights += deposit.divide(Validator.ETHER_TO_WEI).longValueExact();
            }
        }

        byte[] seed = HashUtil.sha3(COINBASE_ARRAY[0]);

        // Create proves and validators
        ProposalProof[] proves = new ProposalProof[COINBASE_ARRAY.length];
        for (int i = 0; i < COINBASE_ARRAY.length; ++i) {
            PrivateKey privateKey = new Ed25519PrivateKey(VRF_SK_ARRAY[i]);
            VrfPrivateKey vrfSk = new VrfPrivateKey(privateKey);
            byte[] vrfPk = vrfSk.generatePublicKey().getEncoded();
            // Must use VrfProve Util to prove with Role Code
            VrfResult vrfResult = VrfProof.Util.prove(VrfProof.ROLE_CODES_PROPOSER, 0, vrfSk, seed);
            VrfProof vrfProof = VrfProof.Util.vrfProof(VrfProof.ROLE_CODES_PROPOSER, 0, vrfPk, seed, vrfResult);

            BlockIdentifier blockIdentifier = new BlockIdentifier(HashUtil.sha3(COINBASE_ARRAY[i]), 1000);
            proves[i] = new ProposalProof(vrfProof, COINBASE_ARRAY[i], blockIdentifier, vrfSk.getSigner());
        }

        // Put them to the manager
        ValidatorManager manager = new ValidatorManager(repository);

        // Test every proposer prove's priority
        final int vds = manager.getValidVds();
        int proposers = 0;
        int priorities = 0;
        for (int i = 0; i < vds; ++i) {
            int priority = manager.getPriority(proves[i], expected);

            System.out.println("priority= " + priority);

            if (priority > 0) {
                ++proposers;
                priorities += priority;
            }
        }

        // Check the total weight
        assertTrue(totalWeights * 2 / ValidatorManager.WEIGHT_UNIT_OF_DEPOSIT == manager.getTotalWeight());
        System.out
                .println("proposers= " + proposers + ", priorities= " + priorities + ", totalWeights= " + totalWeights);

        assert proposers > 1;
        assert priorities > (expected / 3 * 2);
    }

    @Test
    public void testGetOwnerList01() {

        assertTrue(VRF_SK_ARRAY.length == COINBASE_ARRAY.length);
        assertTrue(COINBASE_ARRAY.length == DEPOSIT_ARRAY.length);

//        final Repository cacheTrack = repository.startTracking();
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

        // Put them to the manager
        ValidatorManager manager = new ValidatorManager(repository);

        // Try to get all valid deposit owners
        List<ByteArrayWrapper> validDpsOwnerList = manager.getValidDpsOwnerList();

        for (int i = 0; i < COINBASE_ARRAY.length; ++i) {
            byte[] coinbase = COINBASE_ARRAY[i];
            ByteArrayWrapper wrapper = new ByteArrayWrapper(coinbase);

            boolean result = validDpsOwnerList.remove(wrapper);

            BigInteger deposit = DEPOSIT_ARRAY[i];
            if (deposit.compareTo(DEPOSIT_THRESHOLD) >= 0) {
                assertTrue(result);
            } else {
                assertFalse(result);
            }
        }

        assertTrue(validDpsOwnerList.isEmpty());
    }
}