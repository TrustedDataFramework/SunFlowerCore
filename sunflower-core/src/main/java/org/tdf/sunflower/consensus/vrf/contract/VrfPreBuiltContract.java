package org.tdf.sunflower.consensus.vrf.contract;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import lombok.extern.slf4j.Slf4j;
import org.tdf.common.store.Store;
import org.tdf.common.types.Uint256;
import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.Start;
import org.tdf.sunflower.state.Account;
import org.tdf.sunflower.state.Constants;
import org.tdf.sunflower.state.PreBuiltContract;
import org.tdf.sunflower.types.Header;
import org.tdf.sunflower.types.Transaction;
import org.tdf.sunflower.util.ByteUtil;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.tdf.sunflower.state.Constants.VRF_BIOS_CONTRACT_ADDR;

/**
 * @author mawenpeng Precompiled contract (i.e. bios contract) for VRF
 * consensus. Functions: deposit(), withdraw()
 */

@Slf4j
public class VrfPreBuiltContract implements PreBuiltContract {
    public static final byte[] TOTAL_KEY = "total_deposits".getBytes();
    private Account genesisAccount;
    private Map<byte[], byte[]> genesisStorage;

    @Override
    public Account getGenesisAccount() {
        if (genesisAccount == null) {
            synchronized (VrfPreBuiltContract.class) {
                if (genesisAccount == null) {
                    genesisAccount = Account.emptyContract(Constants.VRF_BIOS_CONTRACT_ADDR_HEX_BYTES);
                }
            }
        }
        return genesisAccount;

    }

    @Override
    public void update(Header header, Transaction transaction, Map<HexBytes, Account> accounts,
                       Store<byte[], byte[]> contractStorage) {

        String methodName = "";
        log.info("++++++>> VrfBiosContract method {}, txn hash {}, nonce {}", methodName, transaction.getHash().toHex(),
                transaction.getNonce());

        if (methodName.trim().isEmpty()) {
            log.error("No method name ");
            return;
        }

        if (methodName.equals("deposit")) {
            try {
                deposit(transaction, accounts, contractStorage);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }

        if (methodName.equals("withdraw")) {
            try {
                withdraw(transaction, accounts, contractStorage);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }

        log.error("Invoking unknown contract method {}", methodName);
    }

    @Override
    public Map<byte[], byte[]> getGenesisStorage() {
        if (genesisStorage == null) {
            synchronized (VrfPreBuiltContract.class) {
                if (genesisStorage == null) {
                    genesisStorage = new HashMap<byte[], byte[]>();
                }
            }
        }
        return genesisStorage;
    }

    private void deposit(Transaction transaction, Map<HexBytes, Account> accounts,
                         Store<byte[], byte[]> contractStorage) throws JsonParseException, JsonMappingException, IOException {

        DepositParams depositParams = parseDepositParams(transaction);
        long amount = depositParams.getDepositAmount();

        if (amount <= 0) {
            log.error("Depositing negative or zero value, amount {}", amount);
            return;
        }

        byte[] fromAddr = transaction.getFromAddress().getBytes();
        Account fromAccount = accounts.get(HexBytes.fromBytes(fromAddr));
        Account contractAccount = accounts.get(HexBytes.fromHex(VRF_BIOS_CONTRACT_ADDR));

        if (fromAccount.getBalance().compareTo(Uint256.of(amount)) < 0) {
            log.error("Deposit amount {} is less than account {} balance {}", amount, fromAddr,
                    fromAccount.getBalance());
            return;
        }

        long deposit = 0;
        long total = 0;
        if (contractStorage.get(fromAddr).isPresent()) {
            deposit = ByteUtil.byteArrayToLong(contractStorage.get(fromAddr).get());
        }
        if (contractStorage.get(TOTAL_KEY).isPresent()) {
            total = ByteUtil.byteArrayToLong(contractStorage.get(TOTAL_KEY).get());
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
        fromAccount.setBalance(fromAccount.getBalance().safeSub(Uint256.of(amount)));
        contractAccount.setBalance(contractAccount.getBalance().safeAdd(Uint256.of(amount)));

        // Update contract storage
        contractStorage.put(fromAddr, ByteUtil.longToBytes(deposit));
        contractStorage.put(TOTAL_KEY, ByteUtil.longToBytes(total));

    }

    private void withdraw(Transaction transaction, Map<HexBytes, Account> accounts,
                          Store<byte[], byte[]> contractStorage) throws JsonParseException, JsonMappingException, IOException {

        WithdrawParams withdrawParams = parseWithdrawParams(transaction);

        long amount = withdrawParams.getWithdrawAmount();
        if (amount <= 0) {
            log.error("Withdrawing negative or zero value, amount {}", amount);
            return;
        }

        byte[] fromAddr = transaction.getFromAddress().getBytes();
        Account fromAccount = accounts.get(HexBytes.fromBytes(fromAddr));
        Account contractAccount = accounts.get(HexBytes.fromHex(VRF_BIOS_CONTRACT_ADDR));

        // Get current address deposit and contract total
        long deposit = 0;
        long total = 0;
        if (contractStorage.get(fromAddr).isPresent()) {
            deposit = ByteUtil.byteArrayToLong(contractStorage.get(fromAddr).get());
        }
        if (contractStorage.get(TOTAL_KEY).isPresent()) {
            total = ByteUtil.byteArrayToLong(contractStorage.get(TOTAL_KEY).get());
        }

        if (deposit < amount) {
            log.error("Withdrawing value {} is larger than current deposit {}", amount, deposit);
            return;
        }

        deposit -= amount;
        total -= amount;

        // Update account balance
        fromAccount.setBalance(fromAccount.getBalance().safeAdd(Uint256.of(amount)));
        contractAccount.setBalance(contractAccount.getBalance().safeSub(Uint256.of(amount)));

        contractStorage.put(fromAddr, ByteUtil.longToBytes(deposit));
        contractStorage.put(TOTAL_KEY, ByteUtil.longToBytes(total));
    }

    private WithdrawParams parseWithdrawParams(Transaction transaction)
            throws JsonParseException, JsonMappingException, IOException {
        byte[] payload = transaction.getPayload().getBytes();
        int methodNameLen = payload[0];
        int paramBytesLen = payload.length - methodNameLen - 1;
        byte[] paramBytes = new byte[paramBytesLen];
        System.arraycopy(payload, 1 + methodNameLen, paramBytes, 0, paramBytesLen);
        WithdrawParams withdrawParams = Start.MAPPER.readValue(paramBytes, WithdrawParams.class);
        return withdrawParams;
    }

    private DepositParams parseDepositParams(Transaction transaction)
            throws JsonParseException, JsonMappingException, IOException {
        byte[] payload = transaction.getPayload().getBytes();
        int methodNameLen = payload[0];
        int paramBytesLen = payload.length - methodNameLen - 1;
        byte[] paramBytes = new byte[paramBytesLen];
        System.arraycopy(payload, 1 + methodNameLen, paramBytes, 0, paramBytesLen);
        DepositParams depositParams = Start.MAPPER.readValue(paramBytes, DepositParams.class);
        return depositParams;
    }
}
