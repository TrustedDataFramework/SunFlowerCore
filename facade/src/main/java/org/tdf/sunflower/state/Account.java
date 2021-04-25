package org.tdf.sunflower.state;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.tdf.common.types.Uint256;
import org.tdf.common.util.HexBytes;
import org.tdf.common.util.IntSerializer;
import org.tdf.sunflower.types.CryptoContext;
import org.tdf.sunflower.vm.abi.ContractABI;

import java.util.Collections;
import java.util.List;


@AllArgsConstructor
@Builder
@Data
public class Account {
    public static final int ADDRESS_SIZE = 20;
    private HexBytes address;
    // for normal account this field is continuous integer
    // for contract account this field is nonce of deploy transaction
    private long nonce;
    // the balance of account
    // for contract account, this field is zero
    @JsonSerialize(using = IntSerializer.class)
    private Uint256 balance;
    // for normal address this field is null
    // for contract address this field is creator of this contract
    // amount in contract call transaction will be transfered to this address
    private HexBytes createdBy;
    // hash code of contract code
    // if the account contains none contract, contract hash will be null
    private byte[] contractHash;
    // root hash of contract db
    // if the account is not contract account, this field will be null
    private byte[] storageRoot;

    // contract abis
    @JsonIgnore
    private List<ContractABI> contractABIs;

    private Account() {

    }

    /**
     * create a fresh new account by address
     *
     * @param address address
     * @return a fresh new account
     */
    public static Account emptyAccount(HexBytes address, Uint256 balance) {
        if (address.size() != ADDRESS_SIZE) throw new RuntimeException("address size should be " + ADDRESS_SIZE);
        return new Account(address, 0, Uint256.ZERO, HexBytes.EMPTY, HexBytes.EMPTY_BYTES, CryptoContext.getEmptyTrieRoot(), Collections.emptyList());
    }


    @Override
    public Account clone() {
        return new Account(address, nonce, balance, createdBy, contractHash, storageRoot, contractABIs);
    }

    public void addBalance(Uint256 amount) {
        this.balance = this.balance.safeAdd(amount);
    }

    public void subBalance(Uint256 amount) {
        if(balance.compareTo(amount) < 0)
            throw new RuntimeException("balance of " + address + " is not enougth");
        this.balance = this.balance.safeSub(amount);
    }
}
