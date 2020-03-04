package org.tdf.sunflower.state;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.tdf.common.store.Store;
import org.tdf.common.util.HexBytes;
import org.tdf.crypto.ed25519.Ed25519;
import org.tdf.lotusvm.ModuleInstance;
import org.tdf.sunflower.account.Address;
import org.tdf.sunflower.vm.abi.Context;
import org.tdf.sunflower.vm.hosts.GasLimit;
import org.tdf.sunflower.vm.hosts.Hosts;

import java.util.Collections;

import static org.tdf.sunflower.ApplicationConstants.ADDRESS_SIZE;

@AllArgsConstructor
@Builder
@Data
public class Account {
    private HexBytes address;

    // for normal address this field is
    private long nonce;
    private long balance;


    // for normal address this field is null
    // for contract address this field is creator of this contract
    private HexBytes createdBy;

    // hash code of contract code
    // if the account contains none contract, binary contract will be null
    private byte[] contractHash;

    // root hash of contract db
    private byte[] storageRoot;

    // TODO: reduce zero content of memory

    private Account(){

    }

    // create a random account
    public static Account getRandomAccount() {
        return builder().address(
                Address.fromPublicKey(Ed25519.generateKeyPair().getPublicKey().getEncoded())
        ).build();
    }

    public Account(HexBytes address, long balance) {
        if(address.size() != ADDRESS_SIZE) throw new RuntimeException("address size should be " + ADDRESS_SIZE);
        this.address = address;
        this.balance = balance;
    }

    public Account(String address) {
        this.address = Address.of(address);
    }



    public boolean containsContract() {
        return contractHash != null && contractHash.length != 0;
    }

    @Override
    public Account clone() {
        return new Account(address, nonce, balance, createdBy, contractHash, storageRoot);
    }
}
