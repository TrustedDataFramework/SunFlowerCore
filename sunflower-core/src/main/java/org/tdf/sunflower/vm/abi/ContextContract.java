package org.tdf.sunflower.vm.abi;

import lombok.Getter;
import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.state.Account;

@Getter
public class ContextContract {
    private HexBytes address;
    private long nonce;
    private HexBytes createdBy;

    public ContextContract(Account a){
        this(a.getAddress(), a.getNonce(), a.getCreatedBy());
    }

    public ContextContract(HexBytes address, long nonce, HexBytes createdBy) {
        this.address = address;
        this.nonce = nonce;
        this.createdBy = createdBy;
    }
}
