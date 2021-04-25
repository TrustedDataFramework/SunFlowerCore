package org.tdf.sunflower.vm;

import org.tdf.common.store.Store;
import org.tdf.common.types.Uint256;
import org.tdf.common.util.HexBytes;
import org.tdf.rlp.RLPList;
import org.tdf.sunflower.state.Bios;
import org.tdf.sunflower.state.PreBuiltContract;
import org.tdf.sunflower.vm.abi.ContractABI;

import java.util.List;
import java.util.Map;


public interface Backend {
    long getHeight();

    HexBytes getParentHash();

    Uint256 getBalance(HexBytes address);

    long getHeaderCreatedAt();

    void setBalance(HexBytes address, Uint256 balance);

    long getNonce(HexBytes address);

    void setNonce(HexBytes address, long nonce);


    List<ContractABI> getABI(HexBytes address);

    void setABI(HexBytes address, List<ContractABI> abi);


    long getInitialGas(int payloadSize);


    Map<HexBytes, PreBuiltContract> getPreBuiltContracts();

    Map<HexBytes, Bios> getBios();

    void dbSet(HexBytes address, byte[] key, byte[] value);

    // return empty byte array if key not found
    byte[] dbGet(HexBytes address, byte[] key);

    boolean dbHas(HexBytes address, byte[] key);

    HexBytes getContractCreatedBy(HexBytes address);

    void setContractCreatedBy(HexBytes address, HexBytes createdBy);

    void dbRemove(HexBytes address, byte[] key);

    byte[] getCode(HexBytes address);

    void setCode(HexBytes address, byte[] code);

    void onEvent(HexBytes address, String eventName, RLPList eventData);

    Map<HexBytes, List<Map.Entry<String, RLPList>>> getEvents();

    boolean isStatic();

    Backend getParentBackend();

    Backend createChild();

    // merge modifications, return the new state root
    byte[] merge();

    default Store<byte[], byte[]> getAsStore(HexBytes address) {
        return new Store<byte[], byte[]>() {
            @Override
            public byte[] get(byte[] bytes) {
                return dbGet(address, bytes);
            }

            @Override
            public void put(byte[] bytes, byte[] bytes2) {
                dbSet(address, bytes, bytes2);
            }

            @Override
            public void remove(byte[] bytes) {
                dbRemove(address, bytes);
            }

            @Override
            public void flush() {
                throw new RuntimeException("not implemented");
            }
        };
    }
}
