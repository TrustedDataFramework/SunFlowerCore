package org.tdf.sunflower.sync;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.tdf.sunflower.state.Account;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class Accounts {
    private int total;
    private List<Account> accounts;
    private boolean traversed;
}
