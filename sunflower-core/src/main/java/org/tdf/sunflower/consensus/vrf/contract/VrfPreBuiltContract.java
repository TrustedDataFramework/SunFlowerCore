package org.tdf.sunflower.consensus.vrf.contract;

import lombok.extern.slf4j.Slf4j;
import org.tdf.common.store.Store;
import org.tdf.common.types.Uint256;
import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.Start;
import org.tdf.sunflower.state.Account;
import org.tdf.sunflower.state.BuiltinContract;
import org.tdf.sunflower.state.Constants;
import org.tdf.sunflower.types.Transaction;
import org.tdf.sunflower.util.ByteUtil;
import org.tdf.sunflower.vm.Backend;
import org.tdf.sunflower.vm.CallData;
import org.tdf.sunflower.vm.abi.Abi;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.tdf.sunflower.state.Constants.VRF_BIOS_CONTRACT_ADDR;

/**
 * @author mawenpeng Precompiled contract (i.e. bios contract) for VRF
 * consensus. Functions: deposit(), withdraw()
 */

@Slf4j
public class VrfPreBuiltContract implements BuiltinContract {
    public static final byte[] TOTAL_KEY = "total_deposits".getBytes();
    private Account genesisAccount;
    private Map<HexBytes, HexBytes> genesisStorage;

    @Override
    public HexBytes getAddress() {
        return Constants.VRF_BIOS_CONTRACT_ADDR_HEX_BYTES;

    }

    @Override
    public byte[] call(Backend backend, CallData callData) {

        String methodName = "";
        log.info("++++++>> VrfBiosContract method {}, txn hash {}, nonce {}", methodName, callData.getTxHash().toHex(),
            callData.getTxNonce());

        if (methodName.trim().isEmpty()) {
            log.error("No method name ");
            return ByteUtil.EMPTY_BYTE_ARRAY;
        }

        if (methodName.equals("deposit")) {
//            try {
//                deposit(transaction, accounts, contractStorage);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
            return ByteUtil.EMPTY_BYTE_ARRAY;
        }

        if (methodName.equals("withdraw")) {
//            try {
//                withdraw(transaction, accounts, contractStorage);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
            return ByteUtil.EMPTY_BYTE_ARRAY;
        }

        log.error("Invoking unknown contract method {}", methodName);
        return ByteUtil.EMPTY_BYTE_ARRAY;
    }

    @Override
    public Abi getAbi() {
        return Abi.fromJson("[]");
    }

    @Override
    public Map<HexBytes, HexBytes> getGenesisStorage() {
        if (genesisStorage == null) {
            synchronized (VrfPreBuiltContract.class) {
                if (genesisStorage == null) {
                    genesisStorage = new HashMap<HexBytes, HexBytes>();
                }
            }
        }
        return genesisStorage;
    }

    private void deposit(Transaction transaction, Map<HexBytes, Account> accounts,
                         Store<byte[], byte[]> contractStorage) throws IOException {

        DepositParams depositParams = parseDepositParams(transaction);
        long amount = depositParams.getDepositAmount();

        if (amount <= 0) {
            log.error("Depositing negative or zero value, amount {}", amount);
            return;
        }

        byte[] fromAddr = transaction.getSender();
        Account fromAccount = accounts.get(HexBytes.fromBytes(fromAddr));
        Account contractAccount = accounts.get(HexBytes.fromHex(VRF_BIOS_CONTRACT_ADDR));

        if (fromAccount.getBalance().compareTo(Uint256.of(amount)) < 0) {
            log.error("Deposit amount {} is less than account {} balance {}", amount, fromAddr,
                fromAccount.getBalance());
            return;
        }

        long deposit = 0;
        long total = 0;
        if (!Store.IS_NULL.test(contractStorage.get(fromAddr))) {
            deposit = ByteUtil.byteArrayToLong(contractStorage.get(fromAddr));
        }
        if (!Store.IS_NULL.test(contractStorage.get(TOTAL_KEY))) {
            total = ByteUtil.byteArrayToLong(contractStorage.get(TOTAL_KEY));
        }

        long formerDeposit = deposit;

        deposit += amount;
        total += amount;

        if (deposit < formerDeposit) {
            log.error("Deposit overflow. Former {}, depositing {}", formerDeposit, amount);
            return;
        }
        if (deposit > total) {
            log.error("Deposit greater than total. Deposit {}, total {}", deposit, total);
            return;
        }

        // Update account balance
        fromAccount.setBalance(fromAccount.getBalance().minus(Uint256.of(amount)));
        contractAccount.setBalance(contractAccount.getBalance().plus(Uint256.of(amount)));

        // Update contract storage
        contractStorage.put(fromAddr, ByteUtil.longToBytes(deposit));
        contractStorage.put(TOTAL_KEY, ByteUtil.longToBytes(total));

    }

    private void withdraw(Transaction transaction, Map<HexBytes, Account> accounts,
                          Store<byte[], byte[]> contractStorage) throws IOException {

        WithdrawParams withdrawParams = parseWithdrawParams(transaction);

        long amount = withdrawParams.getWithdrawAmount();
        if (amount <= 0) {
            log.error("Withdrawing negative or zero value, amount {}", amount);
            return;
        }

        byte[] fromAddr = transaction.getSender();
        Account fromAccount = accounts.get(HexBytes.fromBytes(fromAddr));
        Account contractAccount = accounts.get(HexBytes.fromHex(VRF_BIOS_CONTRACT_ADDR));

        // Get current address deposit and contract total
        long deposit = 0;
        long total = 0;
        if (!Store.IS_NULL.test(contractStorage.get(fromAddr))) {
            deposit = ByteUtil.byteArrayToLong(contractStorage.get(fromAddr));
        }
        if (!Store.IS_NULL.test(contractStorage.get(TOTAL_KEY))) {
            total = ByteUtil.byteArrayToLong(contractStorage.get(TOTAL_KEY));
        }

        if (deposit < amount) {
            log.error("Withdrawing value {} is larger than current deposit {}", amount, deposit);
            return;
        }

        deposit -= amount;
        total -= amount;

        // Update account balance
        fromAccount.setBalance(fromAccount.getBalance().plus(Uint256.of(amount)));
        contractAccount.setBalance(contractAccount.getBalance().minus(Uint256.of(amount)));

        contractStorage.put(fromAddr, ByteUtil.longToBytes(deposit));
        contractStorage.put(TOTAL_KEY, ByteUtil.longToBytes(total));
    }

    private WithdrawParams parseWithdrawParams(Transaction transaction)
        throws IOException {
        byte[] payload = transaction.getData();
        int methodNameLen = payload[0];
        int paramBytesLen = payload.length - methodNameLen - 1;
        byte[] paramBytes = new byte[paramBytesLen];
        System.arraycopy(payload, 1 + methodNameLen, paramBytes, 0, paramBytesLen);
        WithdrawParams withdrawParams = Start.MAPPER.readValue(paramBytes, WithdrawParams.class);
        return withdrawParams;
    }

    private DepositParams parseDepositParams(Transaction transaction)
        throws IOException {
        byte[] payload = transaction.getData();
        int methodNameLen = payload[0];
        int paramBytesLen = payload.length - methodNameLen - 1;
        byte[] paramBytes = new byte[paramBytesLen];
        System.arraycopy(payload, 1 + methodNameLen, paramBytes, 0, paramBytesLen);
        DepositParams depositParams = Start.MAPPER.readValue(paramBytes, DepositParams.class);
        return depositParams;
    }
}
