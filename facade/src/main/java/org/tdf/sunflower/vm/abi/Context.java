package org.tdf.sunflower.vm.abi;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.tdf.common.types.Uint256;
import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.state.Account;
import org.tdf.sunflower.types.Header;
import org.tdf.sunflower.types.Transaction;


@Getter
@AllArgsConstructor
public class Context {
    private final Header header;
    private final Transaction transaction;
    private final Account contractAccount;
    private final HexBytes msgSender;
    private final Uint256 amount;

    public boolean containsTransaction() {
        return transaction != null;
    }
}