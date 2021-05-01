package org.tdf.sunflower.vm;

import org.tdf.common.store.Store;
import org.tdf.common.types.Uint256;
import org.tdf.common.util.HexBytes;
import org.tdf.rlp.RLPList;
import org.tdf.sunflower.state.Bios;
import org.tdf.sunflower.state.PreBuiltContract;

import java.util.List;
import java.util.Map;


public interface Backend {
    default void subBalance(HexBytes addr, Uint256 amount) {
        if (getBalance(addr).compareTo(amount) < 0)
            throw new RuntimeException(String.format("balance of account %s less than %s", addr, amount.value().toString(10)));
        setBalance(addr, getBalance(addr).safeSub(amount));
    }

    default void addBalance(HexBytes addr, Uint256 amount) {
        setBalance(addr, getBalance(addr).safeAdd(amount));
    }

    long getHeight();

    HexBytes getParentHash();

    Uint256 getBalance(HexBytes address);

    long getHeaderCreatedAt();

    void setBalance(HexBytes address, Uint256 balance);

    long getNonce(HexBytes address);

    void setNonce(HexBytes address, long nonce);

    long getInitialGas(int payloadSize);


    Map<HexBytes, PreBuiltContract> getPreBuiltContracts();

    Map<HexBytes, Bios> getBios();

    void dbSet(HexBytes address, HexBytes key, HexBytes value);

    // return empty byte array if key not found
    HexBytes dbGet(HexBytes address, HexBytes key);

    boolean dbHas(HexBytes address, HexBytes key);

    HexBytes getContractCreatedBy(HexBytes address);

    void setContractCreatedBy(HexBytes address, HexBytes createdBy);

    void dbRemove(HexBytes address, HexBytes key);

    HexBytes getCode(HexBytes address);

    void setCode(HexBytes address, HexBytes code);

    void onEvent(HexBytes address, String eventName, RLPList eventData);

    Map<HexBytes, List<Map.Entry<String, RLPList>>> getEvents();

    boolean isStatic();

    Backend getParentBackend();

    Backend createChild();

    // merge modifications, return the new state root
    HexBytes merge();

    default Store<HexBytes, HexBytes> getAsStore(HexBytes address) {
        return new Store<HexBytes, HexBytes>() {
            @Override
            public HexBytes get(HexBytes bytes) {
                return dbGet(address, bytes);
            }

            @Override
            public void put(HexBytes bytes, HexBytes bytes2) {
                dbSet(address, bytes, bytes2);
            }

            @Override
            public void remove(HexBytes bytes) {
                dbRemove(address, bytes);
            }

            @Override
            public void flush() {
                throw new RuntimeException("not implemented");
            }
        };
    }
}
