package org.tdf.sunflower.state;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.tdf.common.types.Uint256;
import org.tdf.common.util.HashUtil;
import org.tdf.common.util.HexBytes;
import org.tdf.common.util.IntSerializer;

public class Account {
    public Account() {

    }

    private long nonce;
    // the balance of account
    // for contract account, this field is zero
    @JsonSerialize(using = IntSerializer.class)
    private Uint256 balance;

    // root hash of contract db
    // if the account is not contract account, this field will be null
    private HexBytes storageRoot;

    // hash code of contract code
    // if the account contains none contract, contract hash will be null
    private HexBytes contractHash;


    public Account(long nonce, Uint256 balance, HexBytes contractHash, HexBytes storageRoot) {
        this.nonce = nonce;
        this.balance = balance;
        this.contractHash = contractHash;
        this.storageRoot = storageRoot;
    }

    /**
     * create a fresh new account by address
     *
     * @param address address
     * @return a fresh new account
     */
    public static Account emptyAccount(Uint256 balance) {
        return new Account(0, balance, HashUtil.EMPTY_DATA_HASH_HEX, HashUtil.EMPTY_TRIE_HASH_HEX);
    }


    @Override
    public Account clone() {
        return new Account(nonce, balance, contractHash, storageRoot);
    }


    public void setNonce(long nonce) {
        this.nonce = nonce;
    }

    public void setBalance(Uint256 balance) {
        this.balance = balance;
    }

    public void setContractHash(HexBytes contractHash) {
        this.contractHash = contractHash;
    }

    public void setStorageRoot(HexBytes storageRoot) {
        this.storageRoot = storageRoot;
    }

    public long getNonce() {
        return nonce;
    }

    public Uint256 getBalance() {
        return balance;
    }

    public HexBytes getContractHash() {
        return contractHash;
    }

    public HexBytes getStorageRoot() {
        return storageRoot;
    }

    public boolean isEmpty() {
        return nonce == 0 && balance.isZero() && contractHash.equals(HashUtil.EMPTY_DATA_HASH_HEX)
            && storageRoot.equals(HashUtil.EMPTY_TRIE_HASH_HEX);
    }
}
