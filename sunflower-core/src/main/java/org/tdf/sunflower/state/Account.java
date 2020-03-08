package org.tdf.sunflower.state;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.tdf.common.util.HexBytes;
import org.tdf.rlp.RLPIgnored;

import static org.tdf.sunflower.ApplicationConstants.ADDRESS_SIZE;

@AllArgsConstructor
@Builder
@Data
public class Account {
    private HexBytes address;

    // for normal account this field is continuous integer
    // for contract account this field is nonce of deploy transaction
    private long nonce;

    // the balance of account
    // for contract account, this field is zero
    private long balance;


    // for normal address this field is null
    // for contract address this field is creator of this contract
    private HexBytes createdBy;

    // hash code of contract code
    // if the account contains none contract, contract hash will be null
    private byte[] contractHash;

    // root hash of contract db
    // if the account is not contract account, this field will be null
    private byte[] storageRoot;

    /**
     * mark the account not persisted before
     */
    @RLPIgnored
    private transient boolean fresh;

    // TODO: reduce zero content of memory

    private Account() {

    }

    public Account(HexBytes address, long balance) {
        if (address.size() != ADDRESS_SIZE) throw new RuntimeException("address size should be " + ADDRESS_SIZE);
        this.address = address;
        this.balance = balance;
    }

    public boolean containsContract() {
        return contractHash != null && contractHash.length != 0;
    }

    @Override
    public Account clone() {
        return new Account(address, nonce, balance, createdBy, contractHash, storageRoot, fresh);
    }
}
