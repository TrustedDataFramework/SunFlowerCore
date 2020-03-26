package org.tdf.sunflower.consensus.vrf.contract;
import static org.tdf.sunflower.state.Constants.VRF_BIOS_CONTRACT_ADDR;

import java.nio.charset.StandardCharsets;

import org.tdf.common.trie.Trie;
import org.tdf.common.util.HexBytes;
import org.tdf.rlp.RLPCodec;
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
 * @author mawenpeng
 * Precompiled contract (i.e. bios contract) for VRF consensus.
 * Functions: deposit(), withdraw(), getDeposit()
 */

@Slf4j
public class VrfBiosContractUpdater implements BiosContractUpdater {

    @Override
    public Account getGenesisAccount() {
        return new Account(HexBytes.fromHex(VRF_BIOS_CONTRACT_ADDR), 0, 0, null, null,
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

        if (methodName.equals("getDeposit")) {
            getDeposit(transaction, account, contractStorage);
        }

        int n = contractStorage.get("key".getBytes(StandardCharsets.UTF_8)).map(RLPCodec::decodeInt).orElse(0) + 1;
        contractStorage.put("key".getBytes(StandardCharsets.UTF_8), RLPCodec.encode(n));
    }

    private void deposit(Transaction transaction, Account account, Trie<byte[], byte[]> contractStorage) {
        // Assuming that transaction amount and account balance have been verified in Transactions.basicValidate().
        // Account has been updated in AccountUpdater.updateContractCall().
        long amount = transaction.getAmount();
        byte[] fromAddr = transaction.getFromAddress().getBytes();
        long deposit = 0;
        if(contractStorage.get(fromAddr).isPresent()) {
            deposit = ByteUtil.byteArrayToLong(contractStorage.get(fromAddr).get());
        }
        deposit += amount;
        contractStorage.put(fromAddr, ByteUtil.longToBytes(deposit));
    }

    private void withdraw(Transaction transaction, Account account, Trie<byte[], byte[]> contractStorage) {

    }

    private void getDeposit(Transaction transaction, Account account, Trie<byte[], byte[]> contractStorage) {

    }
}
