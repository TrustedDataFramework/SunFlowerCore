package org.tdf.sunflower.state;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.tdf.common.types.Uint256;
import org.tdf.common.util.HashUtil;
import org.tdf.common.util.HexBytes;
import org.tdf.common.util.IntSerializer;
import org.tdf.sunflower.types.Transaction;

public class Account {
    public Account() {

    }

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
    private HexBytes contractHash;
    // root hash of contract db
    // if the account is not contract account, this field will be null
    private HexBytes storageRoot;

    public Account(HexBytes address, long nonce, Uint256 balance, HexBytes createdBy, HexBytes contractHash, HexBytes storageRoot) {
        this.address = address;
        this.nonce = nonce;
        this.balance = balance;
        this.createdBy = createdBy;
        this.contractHash = contractHash;
        this.storageRoot = storageRoot;
    }

    /**
     * create a fresh new account by address
     *
     * @param address address
     * @return a fresh new account
     */
    public static Account emptyAccount(HexBytes address, Uint256 balance) {
        if (address.size() != Transaction.ADDRESS_LENGTH)
            throw new RuntimeException("address size should be " + Transaction.ADDRESS_LENGTH);
        return new Account(address, 0, balance, Address.empty(), HashUtil.EMPTY_DATA_HASH_HEX, HashUtil.EMPTY_TRIE_HASH_HEX);
    }


    @Override
    public Account clone() {
        return new Account(address, nonce, balance, createdBy, contractHash, storageRoot);
    }

    public void addBalance(Uint256 amount) {
        this.balance = this.balance.plus(amount);
    }

    public void subBalance(Uint256 amount) {
        if (balance.compareTo(amount) < 0)
            throw new RuntimeException("balance of " + address + " is not enougth");
        this.balance = this.balance.minus(amount);
    }

    public void setAddress(HexBytes address) {
        this.address = address;
    }

    public void setNonce(long nonce) {
        this.nonce = nonce;
    }

    public void setBalance(Uint256 balance) {
        this.balance = balance;
    }

    public void setCreatedBy(HexBytes createdBy) {
        this.createdBy = createdBy;
    }

    public void setContractHash(HexBytes contractHash) {
        this.contractHash = contractHash;
    }

    public void setStorageRoot(HexBytes storageRoot) {
        this.storageRoot = storageRoot;
    }

    public HexBytes getAddress() {
        return address;
    }

    public long getNonce() {
        return nonce;
    }

    public Uint256 getBalance() {
        return balance;
    }

    public HexBytes getCreatedBy() {
        return createdBy;
    }

    public HexBytes getContractHash() {
        return contractHash;
    }

    public HexBytes getStorageRoot() {
        return storageRoot;
    }
}
