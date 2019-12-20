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
package org.tdf.sunflower.vrf.vm;

//import static org.silkroad.util.ByteUtil.EMPTY_BYTE_ARRAY;
//import static org.silkroad.vm.vrf.VrfContracts.DEPOSIT_CONTRACT_ADDR;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;
import static org.tdf.sunflower.consensus.vrf.contract.VrfContracts.DEPOSIT_CONTRACT_ADDR;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;
import org.tdf.sunflower.consensus.vrf.contract.PrecompiledContracts;
import org.tdf.sunflower.consensus.vrf.contract.VrfContracts;
import org.tdf.sunflower.consensus.vrf.core.Validator;
import org.tdf.sunflower.consensus.vrf.core.ValidatorManager;
import org.tdf.sunflower.consensus.vrf.util.VrfConstants;
import org.tdf.sunflower.consensus.vrf.vm.DataWord;
import org.tdf.sunflower.facade.BlockRepository;
import org.tdf.sunflower.service.ConsortiumRepositoryService;
import org.tdf.sunflower.util.ByteUtil;

public class VrfContractsTest {

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
    private static final byte[] EMPTY_BYTE_ARRAY = VrfConstants.ZERO_BYTES.getBytes();

    private static final BigInteger DEPOSIT_THRESHOLD = BigInteger.valueOf(Validator.DEPOSIT_MIN_VALUE)
            .multiply(Validator.ETHER_TO_WEI);
    /* Now we use Wei units as deposit in the unit test */
    private static final BigInteger[] DEPOSIT_ARRAY = new BigInteger[] {
            DEPOSIT_THRESHOLD.add(BigInteger.valueOf(2009)), DEPOSIT_THRESHOLD.add(BigInteger.valueOf(690)),
            DEPOSIT_THRESHOLD.add(BigInteger.valueOf(327)), DEPOSIT_THRESHOLD.add(BigInteger.valueOf(240)),
            DEPOSIT_THRESHOLD.add(BigInteger.valueOf(1398)), DEPOSIT_THRESHOLD.add(BigInteger.valueOf(294)),
            DEPOSIT_THRESHOLD.add(BigInteger.valueOf(598)), DEPOSIT_THRESHOLD.add(BigInteger.valueOf(3958)),
            DEPOSIT_THRESHOLD.add(BigInteger.valueOf(1398)), DEPOSIT_THRESHOLD.add(BigInteger.valueOf(498)) };

    private static BigInteger initValidSize;
    private static int initValidVds;

    private static final DataWord contractAddr = DataWord
            .of("0000000000000000000000000000000000000000000000000000000000000011");

    private static final BlockRepository repository = ConsortiumRepositoryService.NONE;;
//	private static final Repository cacheTrack = repository.startTracking();

    @BeforeClass
    public static void setup() {
        assertTrue(COINBASE_ARRAY.length == DEPOSIT_ARRAY.length);

        // Setup Validator Registration for all coinbases
        for (int i = 0; i < COINBASE_ARRAY.length; ++i) {
            BigInteger deposit = DEPOSIT_ARRAY[i];

            PrecompiledContracts.PrecompiledContract contract = VrfContracts.getContractForAddress(contractAddr,
                    COINBASE_ARRAY[i], deposit.toByteArray(), repository, 0);
            // Call deposit
            byte[] data = Hex.decode("d0e30db0");
            Pair<Boolean, byte[]> result = contract.execute(data);
            assertTrue(result.getLeft() == true);

//			cacheTrack.addBalance(contractAddr.getLast20Bytes(), deposit);
        }

        PrecompiledContracts.PrecompiledContract contract = VrfContracts.getContractForAddress(contractAddr,
                COINBASE_ARRAY[0], null, repository, 0);
        // Call getValidSize
        byte[] data = Hex.decode("4c375f7d");
        Pair<Boolean, byte[]> result = contract.execute(data);
        assertTrue(result.getLeft() == true);

        BigInteger validSize = BigInteger.ZERO;
        for (int i = 0; i < DEPOSIT_ARRAY.length; ++i) {
            validSize = validSize.add(DEPOSIT_ARRAY[i]);
        }
        assertTrue(ByteUtil.bytesToBigInteger(result.getRight()).compareTo(validSize) == 0);

        initValidSize = validSize;

        // Call getValidVds
        data = Hex.decode("f3c07c9f");
        result = contract.execute(data);
        assertTrue(result.getLeft() == true);

        int validVds = 10;
        assertTrue(ByteUtil.byteArrayToInt(result.getRight()) == validVds);

        initValidVds = validVds;

        // print out Validator.DEPOSIT_MIN_VALUE
        BigInteger balance = BigInteger.valueOf(Validator.DEPOSIT_MIN_VALUE)
                .multiply(BigInteger.valueOf(10).pow(Validator.ETHER_POW_WEI));
        System.out.println("DEPOSIT_MIN_WEI: 0x" + balance.toString(16));

//		cacheTrack.commit();
    }

    @AfterClass
    public static void cleanup() {
        assertTrue(0 == 0);
    }

    @Test
    public void testDeposite01() {
        PrecompiledContracts.PrecompiledContract contract = VrfContracts.getContractForAddress(contractAddr,
                COINBASE_ARRAY[0], null, repository, 0);

        // Call deposit with null value
        byte[] data = Hex.decode("d0e30db0");
        Pair<Boolean, byte[]> result = contract.execute(data);
        assertTrue(result.getLeft() == false);
        assertTrue(result.getRight().length == 0);

        byte[] deposit = new byte[] { 0 };
        contract = VrfContracts.getContractForAddress(contractAddr, COINBASE_ARRAY[0], deposit, repository, 0);

        // Call deposit with zero value
        result = contract.execute(data);
        assertTrue(result.getLeft() == false);
    }

    @Test
    public void testDeposite02() {
        final byte[] coinbase = org.spongycastle.util.encoders.Hex.decode("1111ceb4e6f404934d098957d200e803239fdf75");
        BigInteger deposit = BigInteger.valueOf(3981);

        PrecompiledContracts.PrecompiledContract contract = VrfContracts.getContractForAddress(contractAddr, coinbase,
                deposit.toByteArray(), repository, 0);

        // Call deposit
        byte[] data = Hex.decode("d0e30db0");
        Pair<Boolean, byte[]> result = contract.execute(data);
        assertTrue(result.getLeft() == true);
        assertTrue(result.getRight().length == 0);

        contract = VrfContracts.getContractForAddress(contractAddr, coinbase, null, repository, 0);
        // Call getValidSize
        data = Hex.decode("4c375f7d");
        result = contract.execute(data);
        assertTrue(result.getLeft() == true);
        assertTrue(ByteUtil.bytesToBigInteger(result.getRight()).compareTo(initValidSize) == 0);
    }

    @Test
    public void testDeposite03() {
        final byte[] coinbase = org.spongycastle.util.encoders.Hex.decode("2222ceb4e6f404934d098957d200e803239fdf75");
        BigInteger deposit = BigInteger.valueOf(-100);

        PrecompiledContracts.PrecompiledContract contract = VrfContracts.getContractForAddress(contractAddr, coinbase,
                deposit.toByteArray(), repository, 0);

        // Call getValidSize
        byte[] data = Hex.decode("4c375f7d");
        Pair<Boolean, byte[]> result = contract.execute(data);
        assertTrue(result.getLeft() == true);
        BigInteger validSize = new BigInteger(result.getRight());

        // Call deposit
        data = Hex.decode("d0e30db0");
        result = contract.execute(data);
        assertTrue(result.getLeft() == true);
        assertArrayEquals(result.getRight(), EMPTY_BYTE_ARRAY);

        contract = VrfContracts.getContractForAddress(contractAddr, coinbase, null, repository, 0);
        // Call getValidSize
        data = Hex.decode("4c375f7d");
        result = contract.execute(data);
        assertTrue(result.getLeft() == true);
        assertTrue(ByteUtil.bytesToBigInteger(result.getRight()).compareTo(initValidSize) == 0);
    }

    @Test
    public void testDepositPubkey01() {
        final byte[] coinbase = org.spongycastle.util.encoders.Hex.decode("3333e9b4e6f404934d098957d200e803239fdf75");
        final String pubkey = new String("bbb361229922ff28bd992c5bbe344c7afdef76fd18c796618c9e9ab7ebae65d9");
        BigInteger deposit = BigInteger.valueOf(3981);

        PrecompiledContracts.PrecompiledContract contract = VrfContracts.getContractForAddress(contractAddr, coinbase,
                deposit.toByteArray(), repository, 0);

        // Call getValidSize
        byte[] data = Hex.decode("4c375f7d");
        Pair<Boolean, byte[]> result = contract.execute(data);
        assertTrue(result.getLeft() == true);
        BigInteger oldValidSize = new BigInteger(result.getRight());

        // Call depositPubkey
        data = Hex.decode("554f40e0" + pubkey);
        result = contract.execute(data);
        assertTrue(result.getLeft() == true);
        assertTrue(result.getRight().length == 0);

        contract = VrfContracts.getContractForAddress(contractAddr, coinbase, null, repository, 0);
        // Call getValidSize
        data = Hex.decode("4c375f7d");
        result = contract.execute(data);
        assertTrue(result.getLeft() == true);
        assertTrue(ByteUtil.bytesToBigInteger(result.getRight()).compareTo(oldValidSize) == 0);

        // Call getPubkey
        data = Hex.decode("063cd44f");
        result = contract.execute(data);
        assertTrue(result.getLeft() == true);
        assertArrayEquals(result.getRight(), Hex.decode(pubkey));
    }

    @Test
    public void testDepositPubkey02() {
        final byte[] coinbase = org.spongycastle.util.encoders.Hex.decode("4444e9b4e6f404934d098957d200e803239fdf75");
        final String pubkey = new String("bbb361229922ff28bd992c5bbe344c7afdef76fd18c796618c9e9ab7ebae65d9");
        BigInteger deposit = BigInteger.valueOf(-100);

        PrecompiledContracts.PrecompiledContract contract = VrfContracts.getContractForAddress(contractAddr, coinbase,
                deposit.toByteArray(), repository, 0);

        // Call getValidSize
        byte[] data = Hex.decode("4c375f7d");
        Pair<Boolean, byte[]> result = contract.execute(data);
        assertTrue(result.getLeft() == true);
        BigInteger oldValidSize = new BigInteger(result.getRight());

        // Call depositPubkey
        data = Hex.decode("554f40e0" + pubkey);
        result = contract.execute(data);
        assertTrue(result.getLeft() == true);
        assertTrue(result.getRight().length == 0);

        contract = VrfContracts.getContractForAddress(contractAddr, coinbase, null, repository, 0);
        // Call getValidSize
        data = Hex.decode("4c375f7d");
        result = contract.execute(data);
        assertTrue(result.getLeft() == true);
        assertTrue(ByteUtil.bytesToBigInteger(result.getRight()).compareTo(oldValidSize) == 0);

        // Call getPubkey, because negative deposit is force to positive, pubkey is
        // saved
        data = Hex.decode("063cd44f");
        result = contract.execute(data);
        assertTrue(result.getLeft() == true);
        assertArrayEquals(result.getRight(), Hex.decode(pubkey));
    }

    @Test
    public void testGetPubkey1() {
        final byte[] coinbase = org.spongycastle.util.encoders.Hex.decode("5555e9b4e6f404934d098957d200e803239fdf75");
        final String pubkey = new String("bbb361229922ff28bd992c5bbe344c7afdef76fd18c796618c9e9ab7ebae65d9");
        BigInteger deposit = BigInteger.valueOf(3981);

        PrecompiledContracts.PrecompiledContract contract = VrfContracts.getContractForAddress(contractAddr, coinbase,
                deposit.toByteArray(), repository, 0);

        // Call getPubkey
        byte[] data = Hex.decode("063cd44f");
        Pair<Boolean, byte[]> result = contract.execute(data);
        assertTrue(result.getLeft() == true);
        assertTrue(result.getRight().length == 32);
        assertArrayEquals(result.getRight(), DataWord.ZERO.getData());

        // Call depositPubkey
        data = Hex.decode("554f40e0" + pubkey);
        result = contract.execute(data);
        assertTrue(result.getLeft() == true);
        assertTrue(result.getRight().length == 0);

        // Call getPubkey
        data = Hex.decode("063cd44f");
        result = contract.execute(data);
        assertTrue(result.getLeft() == true);
        assertTrue(result.getRight().length == 32);
        assertTrue(Arrays.equals(result.getRight(), Hex.decode(pubkey)));
    }

    @Test
    public void testGetPubkey2() {
        final byte[] coinbase = org.spongycastle.util.encoders.Hex.decode("555555b4e6f404934d098957d200e803239fdf75");
        final String pubkey = new String("bbb361229922ff28bd992c5bbe344c7afdef76fd18c796618c9e9ab7ebae65d9");
        BigInteger deposit = DEPOSIT_THRESHOLD.add(BigInteger.valueOf(2009));

        PrecompiledContracts.PrecompiledContract contract = VrfContracts.getContractForAddress(contractAddr, coinbase,
                deposit.toByteArray(), repository, 0);

        // Call getPubkey
        byte[] data = Hex.decode("063cd44f");
        Pair<Boolean, byte[]> result = contract.execute(data);
        assertTrue(result.getLeft() == true);
        assertTrue(result.getRight().length == 32);
        assertArrayEquals(result.getRight(), DataWord.ZERO.getData());

        // Call depositPubkey
        data = Hex.decode("554f40e0" + pubkey);
        result = contract.execute(data);
        assertTrue(result.getLeft() == true);
        assertTrue(result.getRight().length == 0);

        // Call getPubkey
        data = Hex.decode("063cd44f");
        result = contract.execute(data);
        assertTrue(result.getLeft() == true);
        assertTrue(result.getRight().length == 32);
        assertTrue(Arrays.equals(result.getRight(), Hex.decode(pubkey)));
    }

    @Test
    public void testGetDepositOf() {
        final byte[] coinbase = org.spongycastle.util.encoders.Hex.decode("6666eab4e6f404934d098957d200e803239fdf75");
        final String pubkey = new String("bbb361229922ff28bd992c5bbe344c7afdef76fd18c796618c9e9ab7ebae65d9");
        BigInteger deposit = BigInteger.valueOf(3981);

        PrecompiledContracts.PrecompiledContract contract = VrfContracts.getContractForAddress(contractAddr, coinbase,
                deposit.toByteArray(), repository, 0);

        // Call getDepositOf()
        byte[] data = Hex.decode("57cbcf8c");
        Pair<Boolean, byte[]> result = contract.execute(data);
        assertTrue(result.getLeft() == true);
        assertTrue(result.getRight().length == 32);

        // Call depositPubkey
        data = Hex.decode("554f40e0" + pubkey);
        result = contract.execute(data);
        assertTrue(result.getLeft() == true);
        assertTrue(result.getRight().length == 0);

        // Call getDepositOf()
        data = Hex.decode("57cbcf8c");
        result = contract.execute(data);
        assertTrue(result.getLeft() == true);
        assertTrue(result.getRight().length == 32);
        BigInteger newDeposit = new BigInteger(result.getRight());
        assertTrue(deposit.subtract(newDeposit).signum() == 0);
    }

    @Test
    public void testSetPubkey() {
        final byte[] coinbase = org.spongycastle.util.encoders.Hex.decode("7777deb4e6f404934d098957d200e803239fdf75");
        final String pubkey = new String("bbb361229922ff28bd992c5bbe344c7afdef76fd18c796618c9e9ab7ebae65d9");
        BigInteger deposit = BigInteger.valueOf(3981);

        PrecompiledContracts.PrecompiledContract contract = VrfContracts.getContractForAddress(contractAddr, coinbase,
                deposit.toByteArray(), repository, 0);

        // Call getPubkey
        byte[] data = Hex.decode("063cd44f");
        Pair<Boolean, byte[]> result = contract.execute(data);
        assertTrue(result.getLeft() == true);
        assertTrue(result.getRight().length == 32);
        assertArrayEquals(result.getRight(), DataWord.ZERO.getData());

        // Call setPubkey(bytes32)
        data = Hex.decode("6e7b26b0" + pubkey);
        result = contract.execute(data);
        assertTrue(result.getLeft() == true);
        assertTrue(result.getRight().length == 0);

        // Call getDepositOf()
        data = Hex.decode("57cbcf8c");
        result = contract.execute(data);
        assertTrue(result.getLeft() == true);
        deposit = new BigInteger(result.getRight());
        assertTrue(deposit.signum() == 0);

        // Call getPubkey, because deposit is zero, no pubkey is saved
        data = Hex.decode("063cd44f");
        result = contract.execute(data);
        assertTrue(result.getLeft() == true);
        assertArrayEquals(result.getRight(), DataWord.ZERO.getData());
    }

    @Test
    public void testGetValidSize01() {
        final byte[] coinbase = org.spongycastle.util.encoders.Hex.decode("8888e9b4e6f404934d098957d200e803239fdf75");
        final String pubkey = new String("bbb361229922ff28bd992c5bbe344c7afdef76fd18c796618c9e9ab7ebae65d9");
        BigInteger deposit = BigInteger.valueOf(0);

        PrecompiledContracts.PrecompiledContract contract = VrfContracts.getContractForAddress(contractAddr, coinbase,
                deposit.toByteArray(), repository, 0);

        // Call getValidSize
        byte[] data = Hex.decode("4c375f7d");
        Pair<Boolean, byte[]> result = contract.execute(data);
        assertTrue(result.getLeft() == true);
        BigInteger validSize = new BigInteger(result.getRight());

        // Call depositPubkey
        data = Hex.decode("554f40e0" + pubkey);
        result = contract.execute(data);
        assertTrue(result.getLeft() == false);
        assertTrue(result.getRight().length == 0);

        // Call getValidSize, no change in size
        data = Hex.decode("4c375f7d");
        result = contract.execute(data);
        assertTrue(result.getLeft() == true);
        assertTrue(ByteUtil.bytesToBigInteger(result.getRight()).compareTo(validSize) == 0);
    }

    @Test
    public void testGetValidSize02() {
        final byte[] coinbase = org.spongycastle.util.encoders.Hex.decode("9999e9b4e6f404934d098957d200e803239fdf75");
        final String pubkey = new String("bbb361229922ff28bd992c5bbe344c7afdef76fd18c796618c9e9ab7ebae65d9");
        BigInteger deposit = BigInteger.valueOf(0);

        PrecompiledContracts.PrecompiledContract contract = VrfContracts.getContractForAddress(contractAddr, coinbase,
                deposit.toByteArray(), repository, 0);

        // Call getValidSize
        byte[] data = Hex.decode("4c375f7d");
        Pair<Boolean, byte[]> result = contract.execute(data);
        assertTrue(result.getLeft() == true);
        BigInteger validSize = new BigInteger(result.getRight());

        // Call setPubkey(bytes32)
        data = Hex.decode("6e7b26b0" + pubkey);
        result = contract.execute(data);
        assertTrue(result.getLeft() == true);
        assertTrue(result.getRight().length == 0);

        // Call getValidSize, no change in size
        data = Hex.decode("4c375f7d");
        result = contract.execute(data);
        assertTrue(result.getLeft() == true);
        assertTrue(ByteUtil.bytesToBigInteger(result.getRight()).compareTo(validSize) == 0);
    }

    @Test
    public void testGetValidSize03() {
        final byte[] coinbase = org.spongycastle.util.encoders.Hex.decode("aaaae9b4e6f404934d098957d200e803239fdf75");
        final String pubkey = new String("bbb361229922ff28bd992c5bbe344c7afdef76fd18c796618c9e9ab7ebae65d9");
        BigInteger deposit = DEPOSIT_THRESHOLD.add(BigInteger.valueOf(3981));

        PrecompiledContracts.PrecompiledContract contract = VrfContracts.getContractForAddress(contractAddr, coinbase,
                deposit.toByteArray(), repository, 0);

        // Call getValidSize
        byte[] data = Hex.decode("4c375f7d");
        Pair<Boolean, byte[]> result = contract.execute(data);
        assertTrue(result.getLeft() == true);
        BigInteger validSize = new BigInteger(result.getRight());

        // Call depositPubkey
        data = Hex.decode("554f40e0" + pubkey);
        result = contract.execute(data);
        assertTrue(result.getLeft() == true);
        assertTrue(result.getRight().length == 0);

        contract = VrfContracts.getContractForAddress(contractAddr, coinbase, null, repository, 0);
        // Call getValidSize
        data = Hex.decode("4c375f7d");
        result = contract.execute(data);
        assertTrue(result.getLeft() == true);
        assertTrue(ByteUtil.bytesToBigInteger(result.getRight()).compareTo(initValidSize) == 1);
    }

    @Test
    public void testGetValidSize04() {
        final byte[] coinbase = org.spongycastle.util.encoders.Hex.decode("bbbbe9b4e6f404934d098957d200e803239fdf75");
        final String pubkey = new String("bbb361229922ff28bd992c5bbe344c7afdef76fd18c796618c9e9ab7ebae65d9");
        BigInteger deposit = DEPOSIT_THRESHOLD.add(BigInteger.valueOf(-200));

        PrecompiledContracts.PrecompiledContract contract = VrfContracts.getContractForAddress(contractAddr, coinbase,
                deposit.toByteArray(), repository, 0);

        // Call getValidSize
        byte[] data = Hex.decode("4c375f7d");
        Pair<Boolean, byte[]> result = contract.execute(data);
        assertTrue(result.getLeft() == true);
        BigInteger oldValidSize = ByteUtil.bytesToBigInteger(result.getRight());

        // Call depositPubkey with endowment which is less than DEPOSIT_THRESHOLD
        data = Hex.decode("554f40e0" + pubkey);
        result = contract.execute(data);
        assertTrue(result.getLeft() == true);
        assertTrue(result.getRight().length == 0);

        contract = VrfContracts.getContractForAddress(contractAddr, coinbase, null, repository, 0);
        // Call getValidSize
        data = Hex.decode("4c375f7d");
        result = contract.execute(data);
        assertTrue(result.getLeft() == true);
        assertTrue(ByteUtil.bytesToBigInteger(result.getRight()).compareTo(oldValidSize) == 0);

        deposit = BigInteger.valueOf(200);

        contract = VrfContracts.getContractForAddress(contractAddr, coinbase, deposit.toByteArray(), repository, 0);

        // Call depositPubkey with sum of endowment which is more than DEPOSIT_THRESHOLD
        data = Hex.decode("554f40e0" + pubkey);
        result = contract.execute(data);
        assertTrue(result.getLeft() == true);
        assertTrue(result.getRight().length == 0);

        contract = VrfContracts.getContractForAddress(contractAddr, coinbase, null, repository, 0);
        // Call getValidSize
        data = Hex.decode("4c375f7d");
        result = contract.execute(data);
        assertTrue(result.getLeft() == true);
        assertTrue(ByteUtil.bytesToBigInteger(result.getRight()).compareTo(oldValidSize) == 1);
    }

    @Test
    public void testGetValidVds01() {
        final byte[] coinbase = org.spongycastle.util.encoders.Hex.decode("cccce9b4e6f404934d098957d200e803239fdf75");
        final String pubkey = new String("bbb361229922ff28bd992c5bbe344c7afdef76fd18c796618c9e9ab7ebae65d9");
        BigInteger deposit = DEPOSIT_THRESHOLD.add(BigInteger.valueOf(-200));

        PrecompiledContracts.PrecompiledContract contract = VrfContracts.getContractForAddress(contractAddr, coinbase,
                deposit.toByteArray(), repository, 0);

        // Call getValidVds
        byte[] data = Hex.decode("f3c07c9f");
        Pair<Boolean, byte[]> result = contract.execute(data);
        assertTrue(result.getLeft() == true);
        long oldValidVds = ByteUtil.byteArrayToInt(result.getRight());

        // Call depositPubkey with endowment which is less than DEPOSIT_THRESHOLD
        data = Hex.decode("554f40e0" + pubkey);
        result = contract.execute(data);
        assertTrue(result.getLeft() == true);
        assertTrue(result.getRight().length == 0);

        contract = VrfContracts.getContractForAddress(contractAddr, coinbase, null, repository, 0);
        // Call getValidVds
        data = Hex.decode("f3c07c9f");
        result = contract.execute(data);
        assertTrue(result.getLeft() == true);
        assertTrue(ByteUtil.byteArrayToInt(result.getRight()) == oldValidVds);

        // Call depositPubkey with sum of endowment which is more than DEPOSIT_THRESHOLD
        deposit = BigInteger.valueOf(200);
        contract = VrfContracts.getContractForAddress(contractAddr, coinbase, deposit.toByteArray(), repository, 0);
        data = Hex.decode("554f40e0" + pubkey);
        result = contract.execute(data);
        assertTrue(result.getLeft() == true);
        assertTrue(result.getRight().length == 0);

        contract = VrfContracts.getContractForAddress(contractAddr, coinbase, null, repository, 0);
        // Call getValidVds
        data = Hex.decode("f3c07c9f");
        result = contract.execute(data);
        assertTrue(result.getLeft() == true);
        assertTrue(ByteUtil.byteArrayToInt(result.getRight()) == oldValidVds + 1);
    }

    @Test
    public void testGetValidVds02() {
        long totalWeights = 0;
//		final Repository cacheTrack = repository.startTracking();

        // Setup Validator Registration for all coinbases
        final String pubkey = new String("aaaaa1229922ff28bd992c5bbe344c7afdef76fd18c796618c9e9ab7ebae65d9");
        for (int i = 0; i < COINBASE_ARRAY.length; ++i) {
            BigInteger deposit = DEPOSIT_ARRAY[i];

            PrecompiledContracts.PrecompiledContract contract = VrfContracts.getContractForAddress(contractAddr,
                    COINBASE_ARRAY[i], deposit.toByteArray(), repository, 0);
            // Call depositPubkey
            byte[] data = Hex.decode("554f40e0" + pubkey);
            Pair<Boolean, byte[]> result = contract.execute(data);
            assertTrue(result.getLeft() == true);

//			cacheTrack.addBalance(contractAddr.getLast20Bytes(), deposit);

            // Sum of weight
            if (deposit.compareTo(DEPOSIT_THRESHOLD) >= 0) {
                totalWeights += deposit.divide(Validator.ETHER_TO_WEI).longValueExact();
            }
        }

        // Put them to the manager
        ValidatorManager manager = new ValidatorManager(repository);

        // Test every proposer prove's priority
        final int vds = manager.getValidVds();
        final long repoWeights = manager.getTotalWeight();
        assertTrue(vds == 10);
        assertTrue(totalWeights * 2 == repoWeights);
    }

    @Test
    public void testWithdraw01() {
        final byte[] coinbase = org.spongycastle.util.encoders.Hex.decode("dddde9b4e6f404934d098957d200e803239fdf75");
        final String pubkey = new String("bbb361229922ff28bd992c5bbe344c7afdef76fd18c796618c9e9ab7ebae65d9");
        BigInteger deposit = DEPOSIT_THRESHOLD.add(BigInteger.valueOf(-200));

        PrecompiledContracts.PrecompiledContract contract = VrfContracts.getContractForAddress(contractAddr, coinbase,
                deposit.toByteArray(), repository, 0);

        // Call getValidVds
        byte[] data = Hex.decode("f3c07c9f");
        Pair<Boolean, byte[]> result = contract.execute(data);
        assertTrue(result.getLeft() == true);
        long oldValidVds = ByteUtil.byteArrayToInt(result.getRight());

        // Call depositPubkey with endowment which is less than DEPOSIT_THRESHOLD
        data = Hex.decode("554f40e0" + pubkey);
        result = contract.execute(data);
        assertTrue(result.getLeft() == true);
        assertTrue(result.getRight().length == 0);

        contract = VrfContracts.getContractForAddress(contractAddr, coinbase, null, repository, 0);
        // Call getValidVds
        data = Hex.decode("f3c07c9f");
        result = contract.execute(data);
        assertTrue(result.getLeft() == true);
        assertTrue(ByteUtil.byteArrayToInt(result.getRight()) == oldValidVds);

        // Call depositPubkey with sum of endowment which is more than DEPOSIT_THRESHOLD
        deposit = BigInteger.valueOf(200);
        contract = VrfContracts.getContractForAddress(contractAddr, coinbase, deposit.toByteArray(), repository, 0);
        data = Hex.decode("554f40e0" + pubkey);
        result = contract.execute(data);
        assertTrue(result.getLeft() == true);
        assertTrue(result.getRight().length == 0);

        contract = VrfContracts.getContractForAddress(contractAddr, coinbase, null, repository, 0);
        // Call getValidVds
        data = Hex.decode("f3c07c9f");
        result = contract.execute(data);
        assertTrue(result.getLeft() == true);
        assertTrue(ByteUtil.byteArrayToInt(result.getRight()) == oldValidVds + 1);

        // Call withdraw to reduce deposit and let it be less than DEPOSIT_THRESHOLD
        deposit = BigInteger.valueOf(200);
        DataWord dwDeposit = DataWord.of(deposit.toByteArray());
        contract = VrfContracts.getContractForAddress(contractAddr, coinbase, null, repository, 0);
        data = Hex.decode("2e1a7d4d");
        data = ByteUtil.merge(data, dwDeposit.getData());
        result = contract.execute(data);
        assertTrue(result.getLeft() == true);
        assertTrue(result.getRight().length == 0);

        contract = VrfContracts.getContractForAddress(contractAddr, coinbase, null, repository, 0);
        // Call getValidVds
        data = Hex.decode("f3c07c9f");
        result = contract.execute(data);
        assertTrue(result.getLeft() == true);
        assertTrue(ByteUtil.byteArrayToInt(result.getRight()) == oldValidVds);
    }

    @Test
    public void testGetOwnerList01() {
//		final Repository cacheTrack = repository.startTracking();
        final String pubkey = new String("bbb361229922ff28bd992c5bbe344c7afdef76fd18c796618c9e9ab7ebae65d9");

        // Setup Validator Registration for all coinbases
        for (int i = 0; i < COINBASE_ARRAY.length; ++i) {
            BigInteger deposit = DEPOSIT_ARRAY[i];

            PrecompiledContracts.PrecompiledContract contract = VrfContracts.getContractForAddress(contractAddr,
                    COINBASE_ARRAY[i], deposit.toByteArray(), repository, 0);
            // Call depositPubkey
            byte[] data = Hex.decode("554f40e0" + pubkey);
            Pair<Boolean, byte[]> result = contract.execute(data);
            assertTrue(result.getLeft() == true);

//			cacheTrack.addBalance(contractAddr.getLast20Bytes(), deposit);
        }

        // Try to get all valid deposit owners
        VrfContracts.ValidatorRegistration validatorRegistration = new VrfContracts.ValidatorRegistration(
                DEPOSIT_CONTRACT_ADDR, repository, 0);
        List<DataWord> validDpsOwnerList = validatorRegistration.getValidDpsOwnerList();

        for (int i = 0; i < COINBASE_ARRAY.length; ++i) {
            byte[] coinbase = COINBASE_ARRAY[i];
            DataWord dwCoinbase = DataWord.of(coinbase);

            boolean result = validDpsOwnerList.remove(dwCoinbase);
            assertTrue(result);
        }

        assertTrue(validDpsOwnerList.isEmpty());
    }

    @Test
    public void testGetOwnerList02() {
//		final Repository cacheTrack = repository.startTracking();
        final String pubkey = new String("bbb361229922ff28bd992c5bbe344c7afdef76fd18c796618c9e9ab7ebae65d9");

        // Setup Validator Registration for all coinbases
        for (int i = 0; i < COINBASE_ARRAY.length; ++i) {
            BigInteger deposit = DEPOSIT_ARRAY[i];

            PrecompiledContracts.PrecompiledContract contract = VrfContracts.getContractForAddress(contractAddr,
                    COINBASE_ARRAY[i], deposit.toByteArray(), repository, 0);
            // Call depositPubkey
            byte[] data = Hex.decode("554f40e0" + pubkey);
            Pair<Boolean, byte[]> result = contract.execute(data);
            assertTrue(result.getLeft() == true);

//			cacheTrack.addBalance(contractAddr.getLast20Bytes(), deposit);
        }

        // Call withdraw to remove COINBASE_ARRAY[3]
        int removeIndex = 3;
        BigInteger deposit = DEPOSIT_ARRAY[removeIndex];
        byte[] coinbase = COINBASE_ARRAY[removeIndex];
        DataWord dwDeposit = DataWord.of(deposit.toByteArray());
        PrecompiledContracts.PrecompiledContract contract = VrfContracts.getContractForAddress(contractAddr, coinbase,
                null, repository, 0);
        byte[] data = Hex.decode("2e1a7d4d");
        data = ByteUtil.merge(data, dwDeposit.getData());
        Pair<Boolean, byte[]> result = contract.execute(data);
        assertTrue(result.getLeft() == true);
        assertTrue(result.getRight().length == 0);

        // Try to get all valid deposit owners
        VrfContracts.ValidatorRegistration validatorRegistration = new VrfContracts.ValidatorRegistration(
                DEPOSIT_CONTRACT_ADDR, repository, 0);
        List<DataWord> validDpsOwnerList = validatorRegistration.getValidDpsOwnerList();
        assertTrue(validDpsOwnerList.size() == 9);

        for (int i = 0; i < COINBASE_ARRAY.length; ++i) {
            coinbase = COINBASE_ARRAY[i];
            DataWord dwCoinbase = DataWord.of(coinbase);

            boolean status = validDpsOwnerList.remove(dwCoinbase);
            if (i == removeIndex)
                assertTrue(status == false);
            else
                assertTrue(status == true);
        }

        assertTrue(validDpsOwnerList.isEmpty());
    }

    @Test
    public void testSlashing01() {
//		final Repository cacheTrack = repository.startTracking();
        final String pubkey = new String("ccc361229922ff28bd992c5bbe344c7afdef76fd18c796618c9e9ab7ebae65d9");

        // Setup Validator Registration for all coinbases
        for (int i = 0; i < COINBASE_ARRAY.length; ++i) {
            BigInteger deposit = DEPOSIT_ARRAY[i];

            PrecompiledContracts.PrecompiledContract contract = VrfContracts.getContractForAddress(contractAddr,
                    COINBASE_ARRAY[i], deposit.toByteArray(), repository, 0);
            // Call depositPubkey
            byte[] data = Hex.decode("554f40e0" + pubkey);
            Pair<Boolean, byte[]> result = contract.execute(data);
            assertTrue(result.getLeft() == true);

//			cacheTrack.addBalance(contractAddr.getLast20Bytes(), deposit);
        }

        // Call getValidSize
        PrecompiledContracts.PrecompiledContract contract = VrfContracts.getContractForAddress(contractAddr,
                COINBASE_ARRAY[0], null, repository, 0);
        byte[] data = Hex.decode("4c375f7d");
        Pair<Boolean, byte[]> result = contract.execute(data);
        assertTrue(result.getLeft() == true);
        BigInteger oldValidSize = new BigInteger(result.getRight());

        int slashingIndex = 5;
        DataWord slashingCoinbase = DataWord.of(COINBASE_ARRAY[slashingIndex]);

        // Call getDepositOf()
        contract = VrfContracts.getContractForAddress(contractAddr, slashingCoinbase.getLast20Bytes(), null, repository,
                0);
        data = Hex.decode("57cbcf8c");
        result = contract.execute(data);
        assertTrue(result.getLeft() == true);
        BigInteger deposit = new BigInteger(result.getRight());

        // Call slashing to remove COINBASE_ARRAY[5]
        for (int i = 0; i < COINBASE_ARRAY.length; ++i) {
            contract = VrfContracts.getContractForAddress(contractAddr, COINBASE_ARRAY[i], null, repository, 100);
            data = Hex.decode("90d4582f");
            data = ByteUtil.merge(data, slashingCoinbase.getData());
            result = contract.execute(data);
        }

        // Try to get all valid deposit owners
        VrfContracts.ValidatorRegistration validatorRegistration = new VrfContracts.ValidatorRegistration(
                DEPOSIT_CONTRACT_ADDR, repository, 0);
        List<DataWord> validDpsOwnerList = validatorRegistration.getValidDpsOwnerList();
        assertTrue(validDpsOwnerList.size() == 9);

        for (int i = 0; i < COINBASE_ARRAY.length; ++i) {
            byte[] coinbase = COINBASE_ARRAY[i];
            DataWord dwCoinbase = DataWord.of(coinbase);

            boolean status = validDpsOwnerList.remove(dwCoinbase);
            if (i == slashingIndex)
                assertTrue(status == false);
            else
                assertTrue(status == true);
        }

        assertTrue(validDpsOwnerList.isEmpty());

        // Check deposit of slashing address
        contract = VrfContracts.getContractForAddress(contractAddr, slashingCoinbase.getLast20Bytes(), null, repository,
                0);
        // Call getDepositOf()
        data = Hex.decode("57cbcf8c");
        result = contract.execute(data);
        assertTrue(result.getLeft() == true);
        assertArrayEquals(result.getRight(), DataWord.ZERO.getData());

        // Call getValidSize
        contract = VrfContracts.getContractForAddress(contractAddr, COINBASE_ARRAY[0], null, repository, 0);
        data = Hex.decode("4c375f7d");
        result = contract.execute(data);
        assertTrue(result.getLeft() == true);
        BigInteger newValidSize = new BigInteger(result.getRight());
        assertTrue(oldValidSize.subtract(newValidSize).compareTo(deposit) == 0);
    }

    @Test
    public void testSlashing02() {
//		final Repository cacheTrack = repository.startTracking();
        final String pubkey = new String("ddd361229922ff28bd992c5bbe344c7afdef76fd18c796618c9e9ab7ebae65d9");

        // Setup Validator Registration for all coinbases
        for (int i = 0; i < COINBASE_ARRAY.length; ++i) {
            BigInteger deposit = DEPOSIT_ARRAY[i];

            PrecompiledContracts.PrecompiledContract contract = VrfContracts.getContractForAddress(contractAddr,
                    COINBASE_ARRAY[i], deposit.toByteArray(), repository, 0);
            // Call depositPubkey
            byte[] data = Hex.decode("554f40e0" + pubkey);
            Pair<Boolean, byte[]> result = contract.execute(data);
            assertTrue(result.getLeft() == true);

//			cacheTrack.addBalance(contractAddr.getLast20Bytes(), deposit);
        }

        // Call getSlashedSize
        PrecompiledContracts.PrecompiledContract contract = VrfContracts.getContractForAddress(contractAddr,
                COINBASE_ARRAY[0], null, repository, 0);
        byte[] data = Hex.decode("d60697a2");
        Pair<Boolean, byte[]> result = contract.execute(data);
        assertTrue(result.getLeft() == true);
        BigInteger oldSlashed = new BigInteger(result.getRight());

        int slashingIndex = 6;
        DataWord slashingCoinbase = DataWord.of(COINBASE_ARRAY[slashingIndex]);

        // Call getDepositOf()
        contract = VrfContracts.getContractForAddress(contractAddr, slashingCoinbase.getLast20Bytes(), null, repository,
                0);
        data = Hex.decode("57cbcf8c");
        result = contract.execute(data);
        assertTrue(result.getLeft() == true);
        BigInteger deposit = new BigInteger(result.getRight());

        // Call slashing to remove COINBASE_ARRAY[6]
        for (int i = 0; i < COINBASE_ARRAY.length; ++i) {
            contract = VrfContracts.getContractForAddress(contractAddr, COINBASE_ARRAY[i], null, repository, 100);
            data = Hex.decode("90d4582f");
            data = ByteUtil.merge(data, slashingCoinbase.getData());
            result = contract.execute(data);
        }

        // Try to get all valid deposit owners
        VrfContracts.ValidatorRegistration validatorRegistration = new VrfContracts.ValidatorRegistration(
                DEPOSIT_CONTRACT_ADDR, repository, 0);
        List<DataWord> validDpsOwnerList = validatorRegistration.getValidDpsOwnerList();

        for (int i = 0; i < COINBASE_ARRAY.length; ++i) {
            byte[] coinbase = COINBASE_ARRAY[i];
            DataWord dwCoinbase = DataWord.of(coinbase);

            boolean status = validDpsOwnerList.remove(dwCoinbase);
            if (i == slashingIndex) {
                assertTrue(status == false);
            }
        }

        assertTrue(validDpsOwnerList.isEmpty());

        // Check deposit of slashing address
        contract = VrfContracts.getContractForAddress(contractAddr, slashingCoinbase.getLast20Bytes(), null, repository,
                0);
        // Call getDepositOf()
        data = Hex.decode("57cbcf8c");
        result = contract.execute(data);
        assertTrue(result.getLeft() == true);
        assertArrayEquals(result.getRight(), DataWord.ZERO.getData());

        // Call getSlashedSize
        contract = VrfContracts.getContractForAddress(contractAddr, COINBASE_ARRAY[0], null, repository, 0);
        data = Hex.decode("d60697a2");
        result = contract.execute(data);
        assertTrue(result.getLeft() == true);
        BigInteger newSlashed = new BigInteger(result.getRight());
        assertTrue(newSlashed.subtract(oldSlashed).compareTo(deposit) == 0);
    }
}