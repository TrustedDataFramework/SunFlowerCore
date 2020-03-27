package org.tdf.sunflower.consensus.vrf.contract;

import static org.tdf.sunflower.state.Constants.VRF_BIOS_CONTRACT_ADDR;

import java.util.HashMap;
import java.util.Map;

import org.tdf.common.store.Store;
import org.tdf.common.util.HexBytes;
import org.tdf.rlp.RLPItem;
import org.tdf.sunflower.crypto.CryptoContext;
import org.tdf.sunflower.state.Account;
import org.tdf.sunflower.state.BiosContractUpdater;
import org.tdf.sunflower.types.Header;
import org.tdf.sunflower.types.Transaction;
import org.tdf.sunflower.util.ByteUtil;
import org.tdf.sunflower.vm.abi.Context;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author mawenpeng Precompiled contract (i.e. bios contract) for VRF
 *         consensus. Functions: deposit(), withdraw()
 */

@Slf4j
public class VrfBiosContractUpdater implements BiosContractUpdater {
    private Map<byte[], byte[]> genesisStorage;
    public static final byte[] TOTAL_KEY = "total_deposits".getBytes();

    @Override
    public Account getGenesisAccount() {
        return new Account(HexBytes.fromHex(VRF_BIOS_CONTRACT_ADDR), 0, 0, HexBytes.fromHex(VRF_BIOS_CONTRACT_ADDR),
                null, CryptoContext.digest(RLPItem.NULL.getEncoded()), true);
    }

    @Override
    public void update(Header header, Transaction transaction, Map<HexBytes, Account> accounts,
            Store<byte[], byte[]> contractStorage) {

        String methodName = Context.readMethod(transaction.getPayload());

        if (methodName.trim().isEmpty()) {
            log.error("No method name ");
            return;
        }

        if (methodName.equals("deposit")) {
            deposit(transaction, accounts, contractStorage);
            return;
        }

        if (methodName.equals("withdraw")) {
            withdraw(transaction, accounts, contractStorage);
            return;
        }

        log.error("Invoking unknown contract method {}", methodName);
    }

    @Override
    public Map<byte[], byte[]> getGenesisStorage() {
        if (genesisStorage == null) {
            synchronized (VrfBiosContractUpdater.class) {
                if (genesisStorage == null) {
                    genesisStorage = new HashMap<byte[], byte[]>();
                }
            }
        }
        return genesisStorage;
    }

    private void deposit(Transaction transaction, Map<HexBytes, Account> accounts,
            Store<byte[], byte[]> contractStorage) {
        // Assuming that transaction amount and account balance have been verified in
        // Transactions.basicValidate().
        // Account has been updated in AccountUpdater.updateContractCall().

        long amount = transaction.getAmount();

        if (amount <= 0) {
            log.error("Depositing negative value, amount {}", amount);
            return;
        }

        byte[] fromAddr = transaction.getFromAddress().getBytes();
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

        contractStorage.put(fromAddr, ByteUtil.longToBytes(deposit));
        contractStorage.put(TOTAL_KEY, ByteUtil.longToBytes(total));

    }

    private void withdraw(Transaction transaction, Map<HexBytes, Account> accounts,
            Store<byte[], byte[]> contractStorage) {
        // Assuming that transaction amount and account balance have been verified in
        // Transactions.basicValidate().

        long amount = transaction.getAmount();
        if (amount <= 0) {
            log.error("Withdrawing negative value, amount {}", amount);
            return;
        }

        byte[] fromAddr = transaction.getFromAddress().getBytes();
        Account fromAccount = accounts.get(HexBytes.fromBytes(fromAddr));
        Account contractAccount = accounts.get(HexBytes.fromHex(VRF_BIOS_CONTRACT_ADDR));

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
        fromAccount.setBalance(fromAccount.getBalance() + amount);
        contractAccount.setBalance(contractAccount.getBalance() - amount);

        contractStorage.put(fromAddr, ByteUtil.longToBytes(deposit));
        contractStorage.put(TOTAL_KEY, ByteUtil.longToBytes(total));
    }
}
