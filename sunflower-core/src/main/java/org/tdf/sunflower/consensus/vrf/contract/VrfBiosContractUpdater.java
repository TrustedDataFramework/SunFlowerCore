package org.tdf.sunflower.consensus.vrf.contract;

import static org.tdf.sunflower.state.Constants.VRF_BIOS_CONTRACT_ADDR;

import java.util.HashMap;
import java.util.Map;

import org.tdf.common.trie.Trie;
import org.tdf.common.util.HexBytes;
import org.tdf.rlp.RLPItem;
import org.tdf.sunflower.crypto.CryptoContext;
import org.tdf.sunflower.state.Account;
import org.tdf.sunflower.state.BiosContractUpdater;
import org.tdf.sunflower.types.Header;
import org.tdf.sunflower.types.Transaction;
import org.tdf.sunflower.util.ByteUtil;
import org.tdf.sunflower.vm.abi.Context;

import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author mawenpeng Precompiled contract (i.e. bios contract) for VRF
 *         consensus. Functions: deposit(), withdraw()
 */

@Slf4j
public class VrfBiosContractUpdater implements BiosContractUpdater {
    private Map<byte[], byte[]> genesisStorage;

    @Override
    public Account getGenesisAccount() {
        return new Account(HexBytes.fromHex(VRF_BIOS_CONTRACT_ADDR), 0, 0, HexBytes.fromHex(VRF_BIOS_CONTRACT_ADDR), null,
                CryptoContext.digest(RLPItem.NULL.getEncoded()), true);
    }

    @Override
    public void update(Header header, Transaction transaction, Account account, Trie<byte[], byte[]> contractStorage) {

        Context context = new Context(header, transaction, account, null);
        String methodName = context.getMethod();

        if (methodName == null || methodName == "") {
            log.error("No method name ");
            return;
        }

        if (methodName.equals("deposit")) {
            deposit(transaction, account, contractStorage);
        }

        if (methodName.equals("withdraw")) {
            withdraw(transaction, account, contractStorage);
        }

        log.error("Calling unknown contract method {}", methodName);
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

    private void deposit(Transaction transaction, Account account, Trie<byte[], byte[]> contractStorage) {
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
        if (contractStorage.get(fromAddr).isPresent()) {
            deposit = ByteUtil.byteArrayToLong(contractStorage.get(fromAddr).get());
        }
        long formerDeposit = deposit;
        deposit += amount;

        if (deposit < formerDeposit) {
            log.error("Deposit overflow. Former {}, depositing {}", formerDeposit, amount);
            return;
        }
        contractStorage.put(fromAddr, ByteUtil.longToBytes(deposit));
    }

    private void withdraw(Transaction transaction, Account account, Trie<byte[], byte[]> contractStorage) {
        // Assuming that transaction amount and account balance have been verified in
        // Transactions.basicValidate().
        // Account has been updated in AccountUpdater.updateContractCall().

        long amount = transaction.getAmount();

        if (amount <= 0) {
            log.error("Withdrawing negative value, amount {}", amount);
            return;
        }

        byte[] fromAddr = transaction.getFromAddress().getBytes();
        long deposit = 0;
        if (contractStorage.get(fromAddr).isPresent()) {
            deposit = ByteUtil.byteArrayToLong(contractStorage.get(fromAddr).get());
        }

        if (deposit < amount) {
            log.error("Withdrawing value {} is larger than current deposit {}", amount, deposit);
            return;
        }

        deposit -= amount;

        contractStorage.put(fromAddr, ByteUtil.longToBytes(deposit));
    }
}
