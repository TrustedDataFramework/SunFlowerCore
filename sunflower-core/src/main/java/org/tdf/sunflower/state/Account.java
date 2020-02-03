package org.tdf.sunflower.state;

import lombok.*;
import org.tdf.common.util.HexBytes;
import org.tdf.crypto.ed25519.Ed25519;
import org.tdf.lotusvm.ModuleInstance;
import org.tdf.sunflower.account.Address;
import org.tdf.sunflower.vm.hosts.Hosts;

import java.util.Arrays;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class Account {
    private HexBytes address;
    private long balance;
    // if the account contains none contract, binary contract will be null
    private byte[] binaryContract;

    // TODO: reduce zero content of memory
    private byte[] memory;
    private long[] globals;

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

    public byte[] view(String method, byte[] parameters) throws Exception {
        Hosts hosts = new Hosts().withPayload(parameters);
        ModuleInstance.Builder builder = ModuleInstance.builder()
                .memory(memory)
                .globals(globals)
                .initMemory(false)
                .initGlobals(false)
                .hostFunctions(new Hosts().withPayload(parameters).getAll());
        ModuleInstance instance =
                builder.hostFunctions(hosts.getAll()).build();
        instance.execute(method);
        return hosts.getResult();
    }

    @Override
    public Account clone() {
        return new Account(address, balance, binaryContract,
                memory == null ? null : Arrays.copyOfRange(memory, 0, memory.length),
                globals == null ? null : Arrays.copyOfRange(globals, 0, globals.length)
        );
    }

    public boolean containsContract(){
        return binaryContract != null && binaryContract.length != 0;
    }
}
