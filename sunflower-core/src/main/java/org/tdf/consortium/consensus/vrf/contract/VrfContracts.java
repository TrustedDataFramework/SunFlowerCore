package org.tdf.consortium.consensus.vrf.contract;

import static org.wisdom.consortium.util.ByteUtil.EMPTY_BYTE_ARRAY;
import static org.wisdom.consortium.util.ByteUtil.isNullOrZeroArray;
import static org.wisdom.consortium.util.ByteUtil.parseBytes;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;
import org.tdf.common.BlockRepository;
import org.wisdom.consortium.consensus.vrf.HashUtil;
import org.wisdom.consortium.consensus.vrf.core.Validator;
import org.wisdom.consortium.consensus.vrf.vm.DataWord;
import org.wisdom.consortium.util.ByteUtil;

/**
 * @author James Hu
 * @since 2019/5/21
 */
public class VrfContracts {

    private static final Logger logger = LoggerFactory.getLogger("VrfContracts");

    private static final boolean DEBUG = true;

    // TransactionExecutor use DataWord to store contract address,
    // so we should define contract address as 32 bytes as DataWord required
    public static final DataWord DEPOSIT_CONTRACT_ADDR = DataWord.of("0000000000000000000000000000000000000000000000000000000000000011");

    public static PrecompiledContracts.PrecompiledContract getContractForAddress(DataWord address, byte[] sender, byte[] value, BlockRepository cacheTrack, long blockNum) {
        if (address.equals(DEPOSIT_CONTRACT_ADDR)) {
            DepositContract depositeContract = new DepositContract(sender, value, cacheTrack, blockNum);

            return depositeContract;
        }

        return null;
    }

    /**
     * Precompiled Contract for validator deposit logic
     * Refer to: srchain-core/src/main/resources/solidity/vrf/ValidatorDeposit.sol
     */
    public static class DepositContract extends PrecompiledContracts.PrecompiledContract {

        // function hash code for getValidSize
        private static final byte[] fcnGetValidSize = Hex.decode("4c375f7d");
        // function hash code for getValidVds
        private static final byte[] fcnGetValidVds = Hex.decode("f3c07c9f");
        // function hash code for getPubkey
        private static final byte[] fcnGetPubkey = Hex.decode("063cd44f");
        // function hash code for setPubkey(bytes32)
        private static final byte[] fcnSetPubkey = Hex.decode("6e7b26b0");
        // function hash code for getDepositOf()
        private static final byte[] fcnGetDepositOf = Hex.decode("57cbcf8c");
        // function hash code for depositPubkey(bytes32)
        private static final byte[] fcnDepositPubkey = Hex.decode("554f40e0");
        // function hash code for deposit()
        private static final byte[] fcnDeposit = Hex.decode("d0e30db0");
        // function hash code for withdraw()
        private static final byte[] fcnWithdraw = Hex.decode("2e1a7d4d");
        // function hash code for slashing()
        private static final byte[] fcnSlashing = Hex.decode("90d4582f");
        // function hash code for getSlashedSize()
        private static final byte[] fcnGetSlashedSize = Hex.decode("d60697a2");

        /* The 20 bytes address from which send the transaction */
        private final byte[] sender;
        /* The value data in transaction */
        private final byte[] value;
        /* Validator Registration which is for validator register its deposit for participate consensus */
        private final ValidatorRegistration validatorRegistration;

        public DepositContract(byte[] sender, byte[] value, BlockRepository cacheTrack, long blockNum) {
            this.sender = sender;
            this.value = value;
            this.validatorRegistration = new ValidatorRegistration(DEPOSIT_CONTRACT_ADDR, cacheTrack, blockNum);
        }

        @Override
        public long getGasForData(byte[] data) {

            // Das charge for the execution, raise the gas threshold to increase attack cost
            if (data == null) return 1600;

            return 56000;
        }

        @Override
        public Pair<Boolean, byte[]> execute(byte[] data) {
            if (data == null || data.length < 4) {
                logger.error("Input data length of execute is less than 4");
                return Pair.of(false, EMPTY_BYTE_ARRAY);
            }

            byte[] function = parseBytes(data, 0, 4);
            if (Arrays.equals(function, fcnGetValidSize)) { // getValidSize
                // Total deposits size in the repository track
                BigInteger depositsSize = validatorRegistration.getValidSize();
                if (depositsSize != null) {
                    DataWord dpsBytes = DataWord.of(depositsSize.toByteArray());
                    return Pair.of(true, dpsBytes.getData());
                } else {
                    // Client required uint256 for parsing the return data
                    DataWord dpsBytes = DataWord.ZERO;
                    return Pair.of(true, dpsBytes.getData());
                }
            } else if (Arrays.equals(function, fcnGetDepositOf)) { // getDepositOf()
                BigInteger deposit = validatorRegistration.getDepositOf(sender);
                if (deposit != null) {
                    DataWord dpsBytes = DataWord.of(deposit.toByteArray());
                    return Pair.of(true, dpsBytes.getData());
                } else {
                    // Client required uint256 for parsing the return data
                    DataWord dpsBytes = DataWord.ZERO;
                    return Pair.of(true, dpsBytes.getData());
                }
            } else if (Arrays.equals(function, fcnGetPubkey)) { // getPubkey()
                byte[] vrfPk = validatorRegistration.getPubkey(sender);
                if (vrfPk != null) {
                    return Pair.of(true, vrfPk);
                } else {
                    // Client required bytes32 for parsing the return data
                    DataWord dpsBytes = DataWord.ZERO;
                    return Pair.of(true, dpsBytes.getData());
                }
            } else if (Arrays.equals(function, fcnDepositPubkey)) { // depositPubkey(bytes32)
                if (depositPubkey(sender, value, data)) {
                    return Pair.of(true, EMPTY_BYTE_ARRAY);
                }
            } else if (Arrays.equals(function, fcnDeposit)) { // deposit()
                if (deposit(sender, value)) {
                    return Pair.of(true, EMPTY_BYTE_ARRAY);
                }
            } else if (Arrays.equals(function, fcnSetPubkey)) { // setPubkey(bytes32)
                if (setPubkey(sender, data)) {
                    return Pair.of(true, EMPTY_BYTE_ARRAY);
                }
            } else if (Arrays.equals(function, fcnWithdraw)) { // withdraw()
                if (withdraw(sender, data)) {
                    return Pair.of(true, EMPTY_BYTE_ARRAY);
                }
            } else if (Arrays.equals(function, fcnGetValidVds)) { // getValidVds()
                // Total number of validators in the repository track
                int validVds = validatorRegistration.getValidVds();
                DataWord vdsBytes = DataWord.of(validVds);
                return Pair.of(true, vdsBytes.getData());
            } else if (Arrays.equals(function, fcnSlashing)) { // slashing()
                if (slashing(sender, data)) {
                    return Pair.of(true, EMPTY_BYTE_ARRAY);
                }
            } else if (Arrays.equals(function, fcnGetSlashedSize)) { // getSlashedSize()
                // Total deposits being slashed in the repository track
                BigInteger slashedSize = validatorRegistration.getSlashedSize();
                if (slashedSize != null) {
                    DataWord dpsBytes = DataWord.of(slashedSize.toByteArray());
                    return Pair.of(true, dpsBytes.getData());
                } else {
                    // Client required uint256 for parsing the return data
                    DataWord dpsBytes = DataWord.ZERO;
                    return Pair.of(true, dpsBytes.getData());
                }
            } else {
                logger.error("Unknown function found:{}", Hex.toHexString(function));
            }

            return Pair.of(false, EMPTY_BYTE_ARRAY);
        }

        private boolean depositPubkey(byte[] sender, byte[] value, byte[] data) {
            if (value == null || data.length < 4 + 32) {
                logger.error("Input data length of depositPubkey is less than 36");
                return false;
            }

            // Get payable from sender
            BigInteger endowment = new BigInteger(1, value);
            if (endowment.signum() < 0) {
                logger.error("Input endowment of depositPubkey is negative");
                return false;
            } else if (endowment.signum() == 0) {
                logger.error("Input endowment of depositPubkey is zero");
                return false;
            }

            // New a copy of VRF Public Key
            byte[] vrfPk = parseBytes(data, 4, 32);

            // Save pubkey and deposit of sender to repository track
            return validatorRegistration.depositPubkey(sender, vrfPk, endowment);
        }

        private boolean deposit(byte[] sender, byte[] value) {
            if (isNullOrZeroArray(value)) {
                logger.error("Length of input value for deposit is zero");
                return false;
            }

            // Get payable from sender
            BigInteger endowment = new BigInteger(1, value);
            if (endowment.signum() < 0) {
                logger.error("Input endowment of deposit is negative");
                return false;
            } else if (endowment.signum() == 0) {
                logger.error("Input endowment of deposit is zero");
                return false;
            }

            // Save deposit of sender to repository track
            return validatorRegistration.deposit(sender, endowment);
        }

        private boolean setPubkey(byte[] sender, byte[] data) {
            if (data.length < 4 + 32) {
                logger.error("Input data length of depositPubkey is less than 36");
                return false;
            }

            // New a copy of VRF Public Key
            byte[] vrfPk = parseBytes(data, 4, 32);

            // Save pubkey of sender to repository track
            return validatorRegistration.setPubkey(sender, vrfPk);
        }

        private boolean withdraw(byte[] sender, byte[] data) {
            if (data.length < 4 + 32) {
                logger.error("Input data length of withdraw is less than 36");
                return false;
            }

            // New a copy of withdrawal data bytes
            byte[] wdlBytes = parseBytes(data, 4, 32);
            BigInteger withdrawal = new BigInteger(1, wdlBytes);

            // Withdraw from owner address to sender address in repository track
            return validatorRegistration.withdraw(sender, withdrawal);
        }

        private boolean slashing(byte[] sender, byte[] data) {
            if (data.length < 4 + 32) {
                logger.error("Input data length of slashing is less than 36");
                return false;
            }

            // New a copy of address for slashing
            byte[] addr = parseBytes(data, 16, 20);

            // Do slashing request to specific address
            return validatorRegistration.slashing(sender, addr);
        }
    }

    public static class ValidatorRegistration {

        private static final BigInteger DEPOSIT_UNIT_WEI = BigInteger.valueOf(Validator.DEPOSIT_UNIT_VALUE).multiply(Validator.ETHER_TO_WEI);
        private static final BigInteger DEPOSIT_MIN_WEI  = BigInteger.valueOf(Validator.DEPOSIT_MIN_VALUE).multiply(Validator.ETHER_TO_WEI);
        private static final BigInteger DEPOSIT_MAX_WEI  = BigInteger.valueOf(Validator.DEPOSIT_MAX_VALUE).multiply(Validator.ETHER_TO_WEI);

        // We assume block is mined every 3 seconds, 24 * 60 * 60 / 3 = 28800
        // We set slashing request time cycle to 24 hours for a specific slashing address.
        // In another words, we only confirm deposit size of slashing request in 24 hours after first request is received.
        // After 24 hours, another new slashing request time cycle is triggered, and old slashing request is cleared.
        private static final int SLASHING_REQUEST_TIME_CYCLE_BLOCK_NUMBER = 28800;

        /**
         * We have Deposit Owner array to record every valid validator via index,
         * from which we can retrieve all valid validator for liveness analyzer.

         * If one valid validator is deleted from array, we move tail one to replace the deleted index,
         * so we can maintain the array with every index is valid one from header to tail.
         */

        // Storage data for deposit data of validator, such as K/V = {Sender/Deposit}, K/V = {Sender/Vrf Pubkey}
        private static final byte[] KEY_OF_VDR_DEPOSIT = HashUtil.sha256("VDRDEPOSIT".getBytes());
        private static final byte[] KEY_OF_VDR_VRFPK   = HashUtil.sha256("VDRVRFPK".getBytes());

        // Storage data for total deposit size of valid validator whose deposit is more than DEPOSIT_MIN_WEI
        private static final byte[] KEY_OF_VDR_VALID_SIZE = HashUtil.sha256("VDRVALIDSIZE".getBytes());
        // Storage data for array of valid validator owner,
        // including number of valid validators, and owner address of valid validators
        // With such array, Liveness Analyzer can get all valid validators.
        private static final byte[] KEY_OF_VDR_VALID_ARRAY_LEN   = HashUtil.sha256("VDRVALIDARRAYLEN".getBytes());
        private static final byte[] KEY_OF_VDR_VALID_OWNER_ARRAY = HashUtil.sha256("VDRVALIDOWNERARRAY".getBytes());

        // Storage data for total deposit size of slashing request for specific address,
        // K/V = {Slashing Addr/Deposit Size}
        private static final byte[] KEY_OF_SLASH_REQ_SIZE = HashUtil.sha256("SLASHREQSIZE".getBytes());
        // Storage data for slashing request, such as K/V = {Slashing Addr/Sender Array}, K/V = {Slashing Addr/Array Length}
        // K/V = {Slashing Addr/Time Cycle}
        private static final byte[] KEY_OF_SLASH_REQ_SENDER_ARRAY = HashUtil.sha256("SLASHREQSENDERARRAY".getBytes());
        private static final byte[] KEY_OF_SLASH_REQ_ARRAY_LEN    = HashUtil.sha256("SLASHREQARRAYLEN".getBytes());
        private static final byte[] KEY_OF_SLASH_REQ_TIME_UNTIL   = HashUtil.sha256("SLASHREQTIMEUNTIL".getBytes());
        // Storage data for total deposit size be slashed, it can become reward of network incentive in the future.
        private static final byte[] KEY_OF_SLASHED_DEPOSIT_SIZE = HashUtil.sha256("SLASHEDDEPOSITSIZE".getBytes());

        private final DataWord ownerAddr;
        private final BlockRepository cacheTrack;
        private final long blockNum;

        public ValidatorRegistration(DataWord contractAddr, BlockRepository cacheTrack) {
            this(contractAddr, cacheTrack, 0);
        }

        public ValidatorRegistration(DataWord contractAddr, BlockRepository cacheTrack, long blockNum) {
            this.ownerAddr = contractAddr;
            this.cacheTrack = cacheTrack;
            this.blockNum = blockNum;
        }

        public synchronized boolean depositPubkey(byte[] sender, byte[] vrfPk, final BigInteger endowment) {
            // To make sure it is a valid endowment
            if (!checkEndowment(endowment))
                return false;

            // Get deposit of sender
            BigInteger depositOld = BigInteger.ZERO;
            DepositData vdrData = storageLoadDepositData(sender);
            if (vdrData == null) {
                vdrData = new DepositData(vrfPk, endowment);
            } else {
                depositOld = vdrData.getDeposit();
                if (depositOld.compareTo(DEPOSIT_MAX_WEI) >= 0) {
                    logger.error("Sender 0x{} deposit already reach MAX 0x{}", Hex.toHexString(sender, 0, 3), DEPOSIT_MAX_WEI.toString(16));
                    return false;
                }

                // Increase deposit of sender
                BigInteger deposit = endowment.add(depositOld);

                // Update deposit and pubkey for sender
                vdrData.setDeposit(deposit);
                vdrData.setVrfPk(vrfPk);
            }

            // Save new Validator Deposit to storage
            storageSaveDepositData(sender, vdrData);

            if (DEBUG) logger.debug("depositPubkey, run is OK, sender {}, deposit {}, pubkey {}", Hex.toHexString(sender, 0, 3), vdrData.getDeposit(), Hex.toHexString(vdrData.getVrfPk(), 0, 3));

            // Check changed deposit of valid size
            BigInteger validSizeChanged = validSizeChanged(depositOld, endowment);
            if (validSizeChanged.signum() > 0) {
                // Update deposit of valid size and number of valid validators
                updateSizeVdsArray(validSizeChanged, sender);
            } else if (validSizeChanged.signum() < 0) {
                logger.error("Fatal Error in getting negative validSizeChanged {} when call depositPubkey", validSizeChanged);
            }

            return true;
        }

        public synchronized boolean deposit(byte[] sender, BigInteger endowment) {
            // To make sure it is a valid endowment
            if (!checkEndowment(endowment))
                return false;

            // Get deposit of sender
            BigInteger depositOld = BigInteger.ZERO;
            DepositData vdrData = storageLoadDepositData(sender);
            if (vdrData == null) {
                vdrData = new DepositData(null, endowment);
            } else {
                depositOld = vdrData.getDeposit();
                if (depositOld.compareTo(DEPOSIT_MAX_WEI) >= 0) {
                    logger.error("Sender 0x{} already reach MAX deposit 0x{}", Hex.toHexString(sender, 0, 3), DEPOSIT_MAX_WEI.toString(16));
                    return false;
                }

                // Increase deposit of sender
                BigInteger deposit = endowment.add(depositOld);

                vdrData.setDeposit(deposit);
            }

            // Save new Validator Deposit to storage
            storageSaveDepositData(sender, vdrData);

            if (DEBUG) logger.debug("deposit, run is OK, sender {}, deposit {}", Hex.toHexString(sender, 0, 3), vdrData.getDeposit());

            // Check changed deposit of valid size, increase deposit from old one
            BigInteger validSizeChanged = validSizeChanged(depositOld, endowment);
            if (validSizeChanged.signum() > 0) {
                // Update deposit of valid size, number of valid validators and Deposit Owner array
                updateSizeVdsArray(validSizeChanged, sender);
            } else if (validSizeChanged.signum() < 0) {
                logger.error("Fatal Error in getting negative validSizeChanged {} when call deposit", validSizeChanged);
            }

            return true;
        }

        public synchronized boolean withdraw(byte[] sender, BigInteger withdrawal) {
            // To make sure it is a valid withdrawal
            if (!checkWithdrawal(withdrawal))
                return false;

            // Get deposit of sender
            BigInteger depositOld = BigInteger.ZERO;
            DepositData vdrData = storageLoadDepositData(sender);
            if (vdrData == null) {
                logger.error("No Deposit to withdraw from sender, sender {}", Hex.toHexString(sender, 0, 3));
                return false;
            }

            depositOld = vdrData.getDeposit();
            if (depositOld.compareTo(withdrawal) < 0) {
                logger.error("Sender 0x{} has less Deposit than withdrawal 0x{}", Hex.toHexString(sender, 0, 3), withdrawal.toString(16));
                return false;
            }

            // Decrease deposit of sender
            BigInteger deposit = depositOld.subtract(withdrawal);
            // Save new Validator Deposit to storage
            vdrData.setDeposit(deposit);
            storageSaveDepositData(sender, vdrData);

            if (DEBUG) logger.debug("withdraw, run is OK, sender {}, deposit {}, pubkey {}", Hex.toHexString(sender, 0, 3), vdrData.getDeposit(), Hex.toHexString(vdrData.getVrfPk(), 0, 3));

            // Check changed deposit of valid size, decrease deposit from old one
            BigInteger validSizeChanged = validSizeChanged(depositOld, withdrawal.negate());
            if (validSizeChanged.signum() < 0) {
                // Update deposit of valid size, number of valid validators and Deposit Owner array
                updateSizeVdsArray(validSizeChanged, sender);
            } else if (validSizeChanged.signum() > 0) {
                logger.error("Fatal Error in getting positive validSizeChanged {} when call withdraw", validSizeChanged);
            }

            // Transfer deposit to sender account
            transferBalance(ownerAddr.getLast20Bytes(), sender, withdrawal);

            return true;
        }

        public synchronized boolean setPubkey(byte[] sender, byte[] vrfPk) {
            // Get deposit data of sender
            DepositData vdrData = storageLoadDepositData(sender);
            if (vdrData == null) {
                vdrData = new DepositData(vrfPk, BigInteger.ZERO);
            } else {
                vdrData.setVrfPk(vrfPk);
            }

            // Put new Validator Deposit
            storageSaveDepositData(sender, vdrData);

            if (DEBUG) logger.debug("setPubkey, run is OK, sender {}, deposit {}, pubkey {}", Hex.toHexString(sender, 0, 3), vdrData.getDeposit(), Hex.toHexString(vdrData.getVrfPk(), 0, 3));

            return true;
        }

        public synchronized boolean slashing(byte[] sender, byte[] addr) {
            if (sender == null || sender.length != 20) {
                logger.error("Invalid sender addr, addr {}", Hex.toHexString(sender, 0, 6));
                return false;
            }

            if (addr == null || addr.length != 20) {
                logger.error("Invalid slashing addr, addr {}", Hex.toHexString(addr, 0, 6));
                return false;
            }

            // Get deposit data of slashing address
            DepositData vdrData = storageLoadDepositData(addr);
            if (vdrData == null) {
                logger.info("Deposit of slashing address is Empty, skip slashing request, addr {}", Hex.toHexString(addr, 0, 6));
                return true;
            }

            // Get deposit data of sender
            BigInteger deposit = null;
            vdrData = storageLoadDepositData(sender);
            if (vdrData != null) {
                deposit = vdrData.getDeposit();
            } else {
                logger.error("Sender have no deposit for slashing request, sender {}", Hex.toHexString(sender, 0, 6));
                return false;
            }

            if (deposit.compareTo(DEPOSIT_MIN_WEI) < 0) {
                logger.error("Sender is not a valid validator for slashing request, sender {}, deposit {}", Hex.toHexString(sender, 0, 6), deposit);
                return false;
            }

            long reqTime = storageLoadReqTime(addr);
            if (blockNum > reqTime) {
                // Setup new Slashing Time, clear slashing request sender array and add new one for specific slashing address
                reqTime = blockNum + SLASHING_REQUEST_TIME_CYCLE_BLOCK_NUMBER;
                setupReqData(addr, reqTime, sender, deposit);

                logger.info("Setup new Slashing Request Data for specific slashing address, addr {}, depoist {}, reqTime {}", Hex.toHexString(addr, 0, 3), deposit, reqTime);
            } else {
                // Add a new Slashing Request, and check if deposit size reach the threshold
                if (!addReqData(addr, sender, deposit)) {
                    logger.error("Fail to add new Slashing Request, sender {}, addr {}, deposit {}, reqTime {}", Hex.toHexString(sender, 0, 3), Hex.toHexString(addr, 0, 3), deposit, reqTime);
                    return false;
                };
            }

            if (DEBUG) logger.debug("slashing, run is OK, sender {}, addr {}, deposit {}, reqTime {}", Hex.toHexString(sender, 0, 3), Hex.toHexString(addr, 0, 3), deposit, reqTime);

            return true;
        }

        public synchronized byte[] getPubkey(byte[] sender) {
            // Get deposit data of sender
            DepositData vdrData = storageLoadDepositData(sender);
            if (vdrData != null) {
                byte[] vrfPk = vdrData.getVrfPk();
                if (vrfPk != null && vrfPk.length > 0) {
                    // New a copy of VRF Public Key
                    byte[] bytes = new byte[vrfPk.length];
                    System.arraycopy(vrfPk, 0, bytes, 0, vrfPk.length);

                    if (DEBUG) logger.debug("getPubkey, run is OK, sender {}, deposit {}, pubkey {}", Hex.toHexString(sender, 0, 3), vdrData.getDeposit(), Hex.toHexString(vdrData.getVrfPk(), 0, 3));

                    return bytes;
                }
            }

            if (DEBUG) logger.debug("getPubkey, run is OK, sender {}, pubkey is Empty", Hex.toHexString(sender, 0, 3));

            return null;
        }

        public synchronized BigInteger getDepositOf(byte[] sender) {
            BigInteger deposit = null;

            // Get deposit data of sender
            DepositData vdrData = storageLoadDepositData(sender);
            if (vdrData != null) {
                deposit = vdrData.getDeposit();
            } else {
                logger.debug("Its DepositData is not existed, sender {}", Hex.toHexString(sender, 0, 3));
            }

            if (DEBUG) logger.debug("getDepositOf, run is OK, sender {}, deposit {}",
                    Hex.toHexString(sender, 0, 3), (deposit == null) ? "Empty" : deposit);

            // BigInteger is immutable, just return to user
            return deposit;
        }

        public synchronized BigInteger getValidSize() {
            BigInteger validSize = storageLoadValidSize();

            if (DEBUG) logger.debug("getValidSize, run is OK, ValidSize {}", validSize);

            return validSize;
        }

        public synchronized int getValidVds() {
            int validVds = storageLoadValidVds();

            if (DEBUG) logger.debug("getValidVds, run is OK, validVds {}", validVds);

            return validVds;
        }

        public synchronized List<DataWord> getValidDpsOwnerList() {
            List<DataWord> list = new ArrayList<>();
            int validVds = storageLoadValidVds();
            for (int i = 0; i < validVds; i++) {
                DataWord dwDpsOwner = storageLoadValidDpsOwner(i);
                if (dwDpsOwner != null) {
                    list.add(dwDpsOwner);
                } else {
                    logger.error("Empty Deposit Owner is found, Fatal Error, index {}, validVds {}", i, validVds);
                }
            }

            if (DEBUG) logger.debug("getValidDpsOwnerList, run is OK, validVds {}", validVds);

            return list;
        }

        public synchronized BigInteger getSlashedSize() {
            BigInteger slashedSize = storageLoadSlashedSize();

            if (DEBUG) logger.debug("getSlashedDeposit, run is OK, slashedDeposit {}", slashedSize);

            return slashedSize;
        }

        /**
         *                    DEPOSIT_MIN_WEI                         DEPOSIT_MAX_WEI
         * <------------------------>|<-------------------------------------->|<---------------------------->
         *           zone -1         |                zone 0                  |            zone 1
         *
         * @param deposit
         * @return deposit zone
         */
        private int locateDepositZone(BigInteger deposit) {
            if (deposit.compareTo(DEPOSIT_MIN_WEI) < 0) {
                // It is zone -1 if deposit is less than DEPOSIT_MIN_WEI
                return -1;
            } else  if (deposit.compareTo(DEPOSIT_MAX_WEI) > 0) {
                // It is zone 1 if deposit is more than DEPOSIT_MAX_WEI
                return 1;
            } else {
                // It is zone 0 if deposit is located between DEPOSIT_MIN_WEI and DEPOSIT_MAX_WEI
                return 0;
            }
        }

        private BigInteger validSizeChanged(BigInteger depositOld, BigInteger depositChanged) {
            if (depositChanged.signum() == 0)
                return BigInteger.ZERO;

            // Get new one to check valid size, depositChanged can be positive or negative
            BigInteger depositNew = depositOld.add(depositChanged);

            // Locate zones of old deposit and new deposit
            final int zoneOldDeposit = locateDepositZone(depositOld);
            final int zoneNewDeposit = locateDepositZone(depositNew);

            // Count out how much valid deposit size is changed when old one is changing to new one
            if (zoneOldDeposit < 0 && zoneNewDeposit == 0) {
                /**
                 *                    DEPOSIT_MIN_WEI                         DEPOSIT_MAX_WEI
                 * <------------------------>|<-------------------------------------->|<---------------------------->
                 *           old one         |                new one                 |
                 */
                return depositNew;
            } else if (zoneOldDeposit < 0 && zoneNewDeposit > 0) {
                /**
                 *                    DEPOSIT_MIN_WEI                         DEPOSIT_MAX_WEI
                 * <------------------------>|<-------------------------------------->|<---------------------------->
                 *           old one         |                                        |          new one
                 */
                return DEPOSIT_MAX_WEI;
            } else if (zoneOldDeposit == 0 && zoneNewDeposit > 0) {
                /**
                 *                    DEPOSIT_MIN_WEI                         DEPOSIT_MAX_WEI
                 * <------------------------>|<-------------------------------------->|<---------------------------->
                 *                           |                old one                 |          new one
                 */
                return DEPOSIT_MAX_WEI.subtract(depositOld);
            } else if (zoneOldDeposit == 0 && zoneNewDeposit < 0) {
                /**
                 *                    DEPOSIT_MIN_WEI                         DEPOSIT_MAX_WEI
                 * <------------------------>|<-------------------------------------->|<---------------------------->
                 *           new one         |                old one                 |
                 */
                return depositOld.negate();
            } else if (zoneOldDeposit == 0 && zoneNewDeposit == 0) {
                /**
                 *                    DEPOSIT_MIN_WEI                         DEPOSIT_MAX_WEI
                 * <------------------------>|<-------------------------------------->|<---------------------------->
                 *                           |                old one                 |
                 *                           |                new one                 |
                 */
                return depositChanged;
            } else if (zoneOldDeposit > 0 && zoneNewDeposit < 0) {
                /**
                 *                    DEPOSIT_MIN_WEI                         DEPOSIT_MAX_WEI
                 * <------------------------>|<-------------------------------------->|<---------------------------->
                 *           new one         |                                        |           old one
                 */
                return DEPOSIT_MAX_WEI.negate();
            } else if (zoneOldDeposit > 0 && zoneNewDeposit == 0) {
                /**
                 *                    DEPOSIT_MIN_WEI                         DEPOSIT_MAX_WEI
                 * <------------------------>|<-------------------------------------->|<---------------------------->
                 *                           |                new one                 |           old one
                 */
                return depositNew.subtract(DEPOSIT_MAX_WEI);
            }

            return BigInteger.ZERO;
        }

        // Update deposit of valid size, number of valid validators and Deposit Owner array
        private boolean updateSizeVdsArray(BigInteger validSizeChanged, byte[] sender) {
            if (validSizeChanged == null || validSizeChanged.signum() == 0) {
                logger.error("There is no valid size being changed, quit to update");
                return false;
            }

            int validVds = storageLoadValidVds();
            if (validSizeChanged.signum() < 0) {
                int deleteIndex = getValidDspOwnerIndex(sender);
                if (deleteIndex < 0) {
                    logger.error("Could not find sender to be deleted in Deposit Owner array, quit to update");
                    return false;
                }

                // Try to replace delete one with tail one, and then delete the tail one
                int tailIndex = validVds - 1;
                // Deleted one is the tail index, just delete it from storage
                if (deleteIndex < tailIndex) {
                    DataWord dwTailDpsOwner = storageLoadValidDpsOwner(tailIndex);
                    if (dwTailDpsOwner != null) {
                        // Replace delete one with tail one, then delete the tail one
                        storageSaveValidDpsOwner(deleteIndex, dwTailDpsOwner.getData());
                    } else {
                        logger.error("moveTailDpsOwner, Tail Deposit Owner is empty, This is a bug to be fixed, deleteIndex {}, validVds {}, tailIndex {}", deleteIndex, validVds, tailIndex);
                    }
                } else if (deleteIndex == tailIndex) {
                    if (DEBUG) logger.debug("moveTailDpsOwner, delete index is the tail one and remove it, deleteIndex {}, validVds {}", deleteIndex, validVds);
                } else {
                    logger.error("moveTailDpsOwner, Try to delete index beyond tail one, Fatal Error, deleteIndex {}, validVds {}, tailIndex {}", deleteIndex, validVds, tailIndex);
                }
                storageSaveValidDpsOwner(tailIndex, null);

                // Update number of valid validators and save it to storage
                validVds--;
                storageSaveValidVds(validVds);
            } else {
                int dpsIndex = getValidDspOwnerIndex(sender);
                // Only update this valid validators in case that it is not a valid one
                if (dpsIndex < 0) {
                    // Update number of valid validators and save it to storage
                    validVds++;
                    storageSaveValidVds(validVds);

                    // Add new one as tail of array
                    int tailIndex = validVds - 1;
                    storageSaveValidDpsOwner(tailIndex, sender);
                }
            }

            // Update deposit of valid size and save it to storage
            BigInteger validSize = storageLoadValidSize();
            storageSaveValidSize(validSize.add(validSizeChanged));

            if (DEBUG) logger.debug("updateSizeVdsArray, run is OK, validSizeChanged {}, vds {}", validSizeChanged, validVds);

            return true;
        }

        private void setupReqData(byte[] reqAddr, long reqTime, byte[] reqSender, BigInteger deposit) {
            if (reqAddr == null || reqAddr.length != 20) {
                logger.error("Invalid reqAddr in storageSaveReqData");
                return;
            }

            if (reqSender == null || reqSender.length != 20) {
                logger.error("Invalid reqSender in storageSaveReqData");
                return;
            }

            // Set new Slashing Time for specific slashing address
            storageSaveReqTime(reqAddr, reqTime);

            // Clear slashing request sender array for specific slashing address
            int reqSize = storageLoadReqSds(reqAddr);
            for (int i = 0; i < reqSize; i++) {
                storageSaveReqSender(reqAddr, i,null);
            }

            // Set length of sender array to 1
            storageSaveReqSds(reqAddr, 1);

            // Add Sender to specific slashing address
            storageSaveReqSender(reqAddr, 0, reqSender);

            // Setup initial deposit size of slashing request
            storageSaveReqSize(reqAddr, deposit);
        }

        private void tryToSlashDeposit(byte[] reqAddr) {
            DepositData vdrData = storageLoadDepositData(reqAddr);
            BigInteger depositOld = vdrData.getDeposit();

            logger.info("Try to slash deposit, reqAddr {}, deposit {}", Hex.toHexString(reqAddr, 0, 3), vdrData.getDeposit());

            // Delete slashing deposit from valid information including valid size and valid array
            BigInteger validSizeChanged = validSizeChanged(depositOld, depositOld.negate());
            if (validSizeChanged.signum() < 0) {
                // Update deposit of valid size, number of valid validators and Deposit Owner array
                updateSizeVdsArray(validSizeChanged, reqAddr);
            } else if (validSizeChanged.signum() > 0) {
                logger.error("Fatal Error in getting positive validSizeChanged {} when call slashing", validSizeChanged);
            }
            // Clear deposit of specific slashing address
            storageSaveDepositData(reqAddr, null);
            // Increase deposit size being slashed
            BigInteger slashedDeposit = storageLoadSlashedSize();
            slashedDeposit = slashedDeposit.add(depositOld);
            storageSaveSlashedSize(slashedDeposit);

            // Clear all slashing request data of specific slashing address
            final int reqSds = storageLoadReqSds(reqAddr);
            for (int i = 0; i < reqSds; i++) {
                storageSaveReqSender(reqAddr, i,null);
            }
            storageSaveReqSds(reqAddr, 0);
            storageSaveReqSize(reqAddr, BigInteger.ZERO);
            storageSaveReqTime(reqAddr,0);
        }

        private boolean addReqData(byte[] reqAddr, byte[] reqSender, BigInteger deposit) {
            if (reqAddr == null || reqAddr.length != 20) {
                logger.error("Invalid reqAddr in storageSaveReqData");
                return false;
            }

            if (reqSender == null || reqSender.length != 20) {
                logger.error("Invalid reqSender in storageSaveReqData");
                return false;
            }

            // Check if it is a duplicate slashing request of specific slashing address
            final int reqSds = storageLoadReqSds(reqAddr);
            for (int i = 0; i < reqSds; i++) {
                DataWord dwSender = storageLoadReqSender(reqAddr, i);
                if (Arrays.equals(dwSender.getLast20Bytes(), reqSender)) {
                    logger.error("It is a duplicated Slashing Request, reqSender {}", Hex.toHexString(reqSender, 0, 3));
                    return false;
                }
            }

            // Increase deposit size of slashing request
            BigInteger reqSize = storageLoadReqSize(reqAddr);
            reqSize = deposit.add(reqSize);
            // Check if deposit size reach 1/2 valid size, then we can clear deposit of specific slashing address
            BigInteger validSize = storageLoadValidSize();
            BigInteger factor = BigInteger.valueOf(2);
            if (reqSize.multiply(factor).compareTo(validSize) > 0) {
                // Deposit size reach 1/2 valid size, we can clear deposit of specific slashing address,
                // all relative slashing request data from storage and all relative valid data for the validator.
                tryToSlashDeposit(reqAddr);
            } else {
                // Add a new slashing request to storage

                // Increase length of sender array
                storageSaveReqSds(reqAddr, reqSds + 1);
                // Add Sender to specific slashing address
                storageSaveReqSender(reqAddr, reqSds, reqSender);
                // Update deposit size of slashing request
                storageSaveReqSize(reqAddr, reqSize);
            }

            return true;
        }

        private boolean checkEndowment(final BigInteger endowment) {
            if (endowment.compareTo(DEPOSIT_MAX_WEI) > 0) {
                logger.error("Not allow to input endowment more than 0x{}", DEPOSIT_MAX_WEI.toString(16));
                return false;
            }

            if (endowment.remainder(DEPOSIT_UNIT_WEI).signum() > 0) {
                logger.error("Not allow to input endowment which is not a multiple of DEPOSIT_UNIT_WEI, 0x{}", endowment.toString(16));
                return false;
            }

            return true;
        }

        private boolean checkWithdrawal(final BigInteger withdrawal) {
            if (withdrawal.remainder(DEPOSIT_UNIT_WEI).signum() > 0) {
                logger.error("Not allow to input withdrawal which is not a multiple of DEPOSIT_UNIT_WEI, 0x{}", withdrawal.toString(16));
                return false;
            }

            return true;
        }

        private int getValidDspOwnerIndex(byte[] sender) {
            if (sender == null || sender.length != 20) {
                logger.error("Invalid Sender for index of array");
                return -1;
            }

            int validVds = storageLoadValidVds();
            for (int i = 0; i < validVds; i++) {
                DataWord dwDpsOwner = storageLoadValidDpsOwner(i);
                if (Arrays.equals(sender, dwDpsOwner.getLast20Bytes())) {
                    return i;
                }
            }

            return -1;
        }

        private DepositData storageLoadDepositData(byte[] vdrAddr) {
            // Get deposit from storage
            DataWord dwVrfPk = storageLoad(ownerAddr, HashRoot.of(vdrAddr, KEY_OF_VDR_VRFPK));
            DataWord dwDeposit = storageLoad(ownerAddr, HashRoot.of(vdrAddr, KEY_OF_VDR_DEPOSIT));

            // Both of them is empty, return empty data
            if (dwVrfPk == null && dwDeposit == null)
                return null;

            BigInteger deposit;
            if (dwDeposit != null) {
                deposit = dwDeposit.sValue();
            } else {
                deposit = BigInteger.ZERO;
            }

            byte[] vrfPk = (dwVrfPk == null) ? null : dwVrfPk.getData();
            DepositData vdrData = new DepositData(vrfPk, deposit);

            return vdrData;
        }

        private BigInteger storageLoadValidSize() {
            // Get deposits size of valid validators from storage
            DataWord dwSize = storageLoad(ownerAddr, KEY_OF_VDR_VALID_SIZE);
            if (dwSize == null)
                return BigInteger.ZERO;

            BigInteger depositsSize  = dwSize.sValue();

            return depositsSize;
        }

        private int storageLoadValidVds() {
            // Get number of valid validators from storage
            DataWord dwDds = storageLoad(ownerAddr, KEY_OF_VDR_VALID_ARRAY_LEN);
            if (dwDds == null)
                return 0;

            int value = dwDds.intValueSafe();

            return value;
        }

        // Return DataWord for address of Deposit Owner, DataWord is immutable and easy to get 20 bytes address
        private DataWord storageLoadValidDpsOwner(int index) {
            int validVds = storageLoadValidVds();
            if (index >= validVds) {
                logger.error("Invalid index of Deposit Owner, validVds {}, index {}", validVds, index);
                return null;
            }

            // Try to get Deposit Owner with index from storage
            DataWord dwDpsOwner = storageLoad(ownerAddr, HashRoot.of(KEY_OF_VDR_VALID_OWNER_ARRAY, ByteUtil.intToBytes(index)));

            if (DEBUG) logger.debug("storageLoadValidDpsOwner, index {}, owner {}", index, (dwDpsOwner == null) ? "Empty" : Hex.toHexString(dwDpsOwner.getLast20Bytes(), 0, 3));

            return dwDpsOwner;
        }

        private BigInteger storageLoadReqSize(byte[] addr) {
            // Get deposit size of slashing request for specific slashing address from storage
            DataWord dwSize = storageLoad(ownerAddr, HashRoot.of(KEY_OF_SLASH_REQ_SIZE, addr));
            if (dwSize == null)
                return BigInteger.ZERO;

            BigInteger size = dwSize.sValue();

            return size;
        }

        private long storageLoadReqTime(byte[] addr) {
            // Get finish time of slashing request from storage
            DataWord dwTime = storageLoad(ownerAddr, HashRoot.of(KEY_OF_SLASH_REQ_TIME_UNTIL, addr));
            if (dwTime == null)
                return 0;

            long time = dwTime.longValue();

            return time;
        }

        private int storageLoadReqSds(byte[] addr) {
            // Get number of slashing senders from storage
            DataWord dwSds = storageLoad(ownerAddr, HashRoot.of(KEY_OF_SLASH_REQ_ARRAY_LEN, addr));

            if (dwSds == null)
                return 0;

            int value = dwSds.intValueSafe();

            return value;
        }

        private DataWord storageLoadReqSender(byte[] addr, int index) {
            if (addr == null || addr.length != 20) {
                logger.error("Invalid addr to load in storageLoadReqData");
                return null;
            }

            int reqSds = storageLoadReqSds(addr);
            if (index >= reqSds) {
                logger.error("Invalid index of Slashing Request Data, reqSds {}, index {}", reqSds, index);
                return null;
            }

            // Try to load Slashing Request Sender with index to storage.
            DataWord dwSender = storageLoad(ownerAddr, HashRoot.of(KEY_OF_SLASH_REQ_SENDER_ARRAY, addr, ByteUtil.intToBytes(index)));

            if (DEBUG) logger.debug("storageLoadReqData, addr {}, index {}, sender {}", Hex.toHexString(addr, 0, 3),
                    index, (dwSender == null) ? "Empty" : Hex.toHexString(dwSender.getLast20Bytes(), 0, 3));

            return dwSender;
        }

        private BigInteger storageLoadSlashedSize() {
            // Get slashed deposit from storage
            DataWord dwSlashedDeposit = storageLoad(ownerAddr, KEY_OF_SLASHED_DEPOSIT_SIZE);
            if (dwSlashedDeposit == null)
                return BigInteger.ZERO;

            BigInteger slashedDeposit = dwSlashedDeposit.sValue();

            return slashedDeposit;
        }

        private void storageSaveDepositData(byte[] vdrAddr, DepositData vdrData) {
            if (vdrData == null) {
                // Clear all data if vdrData is null
                storageSave(ownerAddr, HashRoot.of(vdrAddr, KEY_OF_VDR_DEPOSIT), null);
                storageSave(ownerAddr, HashRoot.of(vdrAddr, KEY_OF_VDR_VRFPK), null);
                return;
            }

            // Try to store deposit and pubkey to storage
            BigInteger deposit = vdrData.getDeposit();
            if (deposit.signum() > 0) {
                storageSave(ownerAddr, HashRoot.of(vdrAddr, KEY_OF_VDR_DEPOSIT), deposit.toByteArray());

                byte[] vrfPk = vdrData.getVrfPk();
                storageSave(ownerAddr, HashRoot.of(vdrAddr, KEY_OF_VDR_VRFPK), vrfPk);
            } else if (deposit.signum() == 0) {
                // Clear all data if deposit is zero
                storageSave(ownerAddr, HashRoot.of(vdrAddr, KEY_OF_VDR_DEPOSIT), null);
                storageSave(ownerAddr, HashRoot.of(vdrAddr, KEY_OF_VDR_VRFPK), null);
            } else {
                logger.error("Fatal Error with Negative Deposit {} to call storageSaveDepositData", deposit);
            }
        }

        private void storageSaveValidSize(BigInteger depositsSize) {
            if (depositsSize == null) {
                storageSave(ownerAddr, KEY_OF_VDR_VALID_SIZE, null);
                return;
            }

            // Try to store deposits size to storage
            if (depositsSize.signum() > 0) {
                storageSave(ownerAddr, KEY_OF_VDR_VALID_SIZE, depositsSize.toByteArray());
            } else {
                storageSave(ownerAddr, KEY_OF_VDR_VALID_SIZE, null);
            }
        }

        private void storageSaveValidVds(int vds) {
            if (vds < 0)
                return;

            // Try to store number of validators to storage
            byte[] valueBytes = null;
            if (vds > 0) {
                valueBytes = ByteUtil.intToBytes(vds);
            }
            storageSave(ownerAddr, KEY_OF_VDR_VALID_ARRAY_LEN, valueBytes);
        }

        private void storageSaveValidDpsOwner(int index, byte[] dpsOwner) {
            int validVds = storageLoadValidVds();
            if (index >= validVds) {
                logger.error("Invalid index of Deposit Owner, validVds {}, index {}", validVds, index);
                return;
            }

            // Try to save Deposit Owner with index to storage.
            // If dpsOwner is null, it will be deleted from storage.
            storageSave(ownerAddr, HashRoot.of(KEY_OF_VDR_VALID_OWNER_ARRAY, ByteUtil.intToBytes(index)), dpsOwner);
        }

        private void storageSaveReqSize(byte[] addr, BigInteger reqSize) {
            if (reqSize == null)
                return;

            // Try to store deposits size to storage
            if (reqSize.signum() > 0) {
                storageSave(ownerAddr, HashRoot.of(KEY_OF_SLASH_REQ_SIZE, addr), reqSize.toByteArray());
            } else {
                storageSave(ownerAddr, HashRoot.of(KEY_OF_SLASH_REQ_SIZE, addr), null);
            }
        }

        private void storageSaveReqTime(byte[] addr, long reqTime) {
            if (reqTime < 0)
                return;

            // Try to store finish time of request to storage
            byte[] valueBytes = null;
            if (reqTime > 0) {
                valueBytes = ByteUtil.longToBytes(reqTime);
            }
            storageSave(ownerAddr, HashRoot.of(KEY_OF_SLASH_REQ_TIME_UNTIL, addr), valueBytes);
        }

        private void storageSaveReqSds(byte[] addr, int sds) {
            if (sds < 0)
                return;

            // Try to store number of validators to storage
            byte[] valueBytes = null;
            if (sds > 0) {
                valueBytes = ByteUtil.intToBytes(sds);
            }
            storageSave(ownerAddr, HashRoot.of(KEY_OF_SLASH_REQ_ARRAY_LEN, addr), valueBytes);
        }

        private void storageSaveReqSender(byte[] reqAddr, int index, byte[] reqSender) {
            if (reqAddr == null || reqAddr.length != 20) {
                logger.error("Invalid reqAddr in storageSaveReqData");
                return;
            }

            if (reqSender != null && reqSender.length != 20) {
                logger.error("Invalid reqSender in storageSaveReqData");
                return;
            }

            if (blockNum < 0) {
                logger.error("Negative blockNum in storageSaveReqData");
                return;
            }

            long reqTime = storageLoadReqTime(reqAddr);
            if (blockNum > reqTime) {
                logger.error("Invalid blockNum in storageSaveReqData, blockNum {}, reqTime {}", blockNum, reqTime);
                return;
            }

            int reqSds = storageLoadReqSds(reqAddr);
            if (index >= reqSds) {
                logger.error("Invalid index of Request Data, reqSds {}, index {}", reqSds, index);
                return;
            }

            // Try to save Slashing Request Data with index to storage.
            // If data is null, it will be deleted from storage.
            storageSave(ownerAddr, HashRoot.of(KEY_OF_SLASH_REQ_SENDER_ARRAY, reqAddr, ByteUtil.intToBytes(index)), reqSender);
        }

        private void storageSaveSlashedSize(BigInteger slashedDeposit) {
            if (slashedDeposit == null) {
                storageSave(ownerAddr, KEY_OF_SLASHED_DEPOSIT_SIZE, null);
                return;
            }

            // Try to store deposits size to storage
            if (slashedDeposit.signum() > 0) {
                storageSave(ownerAddr, KEY_OF_SLASHED_DEPOSIT_SIZE, slashedDeposit.toByteArray());
            } else {
                storageSave(ownerAddr, KEY_OF_SLASHED_DEPOSIT_SIZE, null);
            }
        }

        private void storageSave(DataWord ownerAddr, byte[] key, byte[] val) {
            if (key == null || key.length == 0)
                return;

            // Save data to storage, and ZERO of val will delete it from storage.
            // It is DataWord immutability, internal data of DataWord is copied from constructor input data.
            DataWord keyWord = DataWord.of(key);
            DataWord valWord = DataWord.of(val);
            //----->  cacheTrack.addStorageRow(ownerAddr.getLast20Bytes(), keyWord, valWord);
        }

        private DataWord storageLoad(DataWord ownerAddr, byte[] key) {
            if (key == null || key.length == 0)
                return null;

            // Load data from storage
            DataWord keyWord = DataWord.of(key);
            DataWord valWord = null; //----->cacheTrack.getStorageValue(ownerAddr.getLast20Bytes(), keyWord);

            if (valWord == null)
                return null;

            // It is DataWord immutability, new data is copied from DataWord
            return valWord;
        }

        private boolean transferBalance(byte[] ownerAddr, byte[] sender, BigInteger value) {
            if (value.signum() <= 0) {
                logger.error("Invalid value to transfer balance, value {}", value);
                return false;
            }

            BigInteger ownerBalance = null; //----->cacheTrack.getBalance(ownerAddr);
            if (ownerBalance.compareTo(value) < 0) {
                logger.error("Balance of contract is less than transfer value, owner {}, value {}", ownerBalance, value);
                return false;
            }

//----->            cacheTrack.addBalance(ownerAddr, value.negate());
//----->            cacheTrack.addBalance(sender, value);

            return true;
        }

        private static class HashRoot {
            public static byte[] of(byte[] key1, byte[] key2) {
                return HashUtil.sha3(key1, key2);
            }
            public static byte[] of(byte[] key1, byte[] key2,  byte[] key3) {
                return HashUtil.sha3(HashUtil.sha3(key1, key2), key3);
            }
        }
    }
}