package org.tdf.sunflower.sync;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.state.Account;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class SyncAccount {
    // the account itself
    private Account account;
    // contract code of contract account
    private HexBytes contractCode;
    // the contract storage of the account
    private List<HexBytes> contractStorage;
}
