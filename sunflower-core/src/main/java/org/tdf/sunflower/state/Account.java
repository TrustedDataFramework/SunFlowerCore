package org.tdf.sunflower.state;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.tdf.common.util.HexBytes;
import org.tdf.crypto.ed25519.Ed25519;
import org.tdf.lotusvm.ModuleInstance;
import org.tdf.sunflower.account.Address;
import org.tdf.sunflower.vm.abi.Context;
import org.tdf.sunflower.vm.hosts.Hosts;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class Account {
    private HexBytes address;

    // for normal address this field is
    private long nonce;
    private long balance;
    // if the account contains none contract, binary contract will be null
    private byte[] binaryContract;

    // TODO: reduce zero content of memory
    private byte[] memory;
    private long[] globals;

    // for normal address this field is null
    // for contract address this field is creator of this contract
    private HexBytes createdBy;

    // create a random account
    public static Account getRandomAccount() {
        return builder().address(
                Address.fromPublicKey(Ed25519.generateKeyPair().getPublicKey().getEncoded())
        ).build();
    }

    public Account(HexBytes address, long balance) {
        this.address = address;
        this.balance = balance;
    }

    public Account(String address) {
        this.address = Address.of(address);
    }

    public byte[] view(String method, byte[] parameters) {
        Context ctx = Context.disabled();
        ctx.setContractAddress(address);
        ctx.setCreatedBy(createdBy);

        Hosts hosts = new Hosts().withPayload(parameters).withContext(ctx);
        ModuleInstance.Builder builder = ModuleInstance.builder()
                .memory(memory)
                .globals(globals)

                .hostFunctions(new Hosts().withPayload(parameters).getAll());
        ModuleInstance instance =
                builder.hostFunctions(hosts.getAll()).build();
        instance.execute(method);
        return hosts.getResult();
    }

    public boolean containsContract() {
        return binaryContract != null && binaryContract.length != 0;
    }

    @Override
    public Account clone() {
        return new Account(address, nonce, balance, binaryContract, memory, globals, createdBy);
    }
}
