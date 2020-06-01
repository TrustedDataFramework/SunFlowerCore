package org.tdf.sunflower.state;

import org.tdf.common.store.Store;
import org.tdf.common.util.HexBytes;
import org.tdf.rlp.RLPCodec;
import org.tdf.sunflower.types.Header;
import org.tdf.sunflower.types.Transaction;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.tdf.sunflower.state.Constants.SIMPLE_BIOS_CONTRACT_ADDR;

public class SimplePreBuiltContract implements PreBuiltContract {
    @Override
    public Account getGenesisAccount() {
        return Account.emptyContract(SIMPLE_BIOS_CONTRACT_ADDR);
    }

    @Override
    public void update(Header header, Transaction transaction, Map<HexBytes, Account> accounts, Store<byte[], byte[]> contractStorage) {
        int n = contractStorage
                .get("key".getBytes(StandardCharsets.UTF_8))
                .map(RLPCodec::decodeInt)
                .orElse(0) + 1;
        contractStorage.put("key".getBytes(StandardCharsets.UTF_8),
                RLPCodec.encode(n));
    }
}
