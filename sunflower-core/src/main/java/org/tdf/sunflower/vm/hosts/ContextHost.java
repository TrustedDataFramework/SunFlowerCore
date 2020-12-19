package org.tdf.sunflower.vm.hosts;

import org.tdf.common.store.Store;
import org.tdf.common.trie.Trie;
import org.tdf.common.util.HexBytes;
import org.tdf.lotusvm.runtime.HostFunction;
import org.tdf.lotusvm.types.FunctionType;
import org.tdf.lotusvm.types.ValueType;
import org.tdf.rlp.RLPCodec;
import org.tdf.sunflower.state.Account;
import org.tdf.sunflower.vm.abi.AbiDataType;
import org.tdf.sunflower.vm.abi.Context;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;

public class ContextHost extends HostFunction {
    private Context context;
    private Map<HexBytes, Account> states;
    private Store<byte[], byte[]> contractCodeStore;
    private Function<byte[], Trie<byte[], byte[]>> storageTrieSupplier;
    private boolean readonly;

    public ContextHost(
            Context context,
            Map<HexBytes, Account> states,
            Store<byte[], byte[]> contractCodeStore,
            Function<byte[], Trie<byte[], byte[]>> storageTrieSupplier,
            boolean readonly
    ) {
        setName("_context");
        setType(
                new FunctionType(Arrays.asList(ValueType.I64, ValueType.I64),
                        Collections.singletonList(ValueType.I64))
        );
        this.context = context;
        this.states = states;
        this.contractCodeStore = contractCodeStore;
        this.storageTrieSupplier = storageTrieSupplier;
        this.readonly = readonly;
    }

    @Override
    public long execute(long... parameters) {
        Type type = Type.values()[(int) parameters[0]];
        if ((type != Type.CONTRACT_ADDRESS
                && type != Type.CONTRACT_NONCE
                && type != Type.CONTRACT_CREATED_BY
                && type != Type.ACCOUNT_NONCE
                && type != Type.ACCOUNT_BALANCE
                && type != Type.CONTRACT_CODE
                && type != Type.CONTRACT_ABI
        ) && readonly) {
            throw new RuntimeException("not available here");
        }
        switch (type) {
            case HEADER_PARENT_HASH: {
                return WBI
                        .mallocBytes(getInstance(), context.getHeader().getHashPrev().getBytes());

            }
            case HEADER_CREATED_AT: {
                return context.getHeader().getCreatedAt();
            }
            case HEADER_HEIGHT: {
                return context.getHeader().getHeight();
            }
            case TX_TYPE: {
                return context.getTransaction().getType();
            }
            case TX_CREATED_AT: {
                return context.getTransaction().getCreatedAt();
            }
            case TX_NONCE: {
                return context.getTransaction().getNonce();
            }
            case TX_ORIGIN: {
                return WBI.mallocAddress(getInstance(), context.getTransaction().getFromAddress().getBytes());
            }
            case TX_GAS_PRICE: {
                return WBI.malloc(getInstance(), context.getTransaction().getGasPrice());
            }
            case TX_AMOUNT: {
                return WBI.malloc(getInstance(), context.getTransaction().getAmount());
            }
            case TX_TO: {
                return WBI.mallocAddress(getInstance(), context.getTransaction().getTo().getBytes());
            }
            case TX_SIGNATURE: {
                return WBI.mallocBytes(getInstance(), context.getTransaction().getSignature().getBytes());
            }
            case TX_HASH: {
                return WBI.mallocBytes(getInstance(), context.getTransaction().getHash().getBytes());
            }
            case CONTRACT_ADDRESS: {
                return WBI
                        .mallocAddress(getInstance(), context.getContractAccount().getAddress().getBytes());
            }
            case CONTRACT_NONCE: {
                return context.getContractAccount().getNonce();
            }
            case CONTRACT_CREATED_BY: {
                return WBI.mallocAddress(getInstance(), context.getContractAccount().getCreatedBy().getBytes());
            }
            case ACCOUNT_NONCE: {
                byte[] addr = (byte[]) WBI
                        .peek(getInstance(), (int) parameters[1], AbiDataType.ADDRESS);
                Account a = states.get(HexBytes.fromBytes(addr));
                return a.getNonce();
            }
            case ACCOUNT_BALANCE: {
                byte[] addr = (byte[]) WBI
                        .peek(getInstance(), (int) parameters[1], AbiDataType.ADDRESS);
                Account a = states.get(HexBytes.fromBytes(addr));
                return WBI.malloc(getInstance(), a.getBalance());
            }
            case MSG_SENDER: {
                return WBI.mallocAddress(getInstance(), context.getMsgSender().getBytes());
            }
            case MSG_AMOUNT: {
                return
                        WBI.malloc(getInstance(), context.getAmount());
            }
            case CONTRACT_CODE: {
                byte[] addr = (byte[]) WBI
                        .peek(getInstance(), (int) parameters[1], AbiDataType.ADDRESS);
                Account a = states.get(HexBytes.fromBytes(addr));
                if (a.getContractHash() == null || a.getContractHash().length == 0)
                    throw new RuntimeException(HexBytes.fromBytes(addr) + " is not a contract account");
                byte[] code = this.contractCodeStore.get(a.getContractHash()).get();
                return WBI.mallocBytes(getInstance(), code);
            }
            case CONTRACT_ABI: {
                byte[] addr = (byte[]) WBI
                        .peek(getInstance(), (int) parameters[1], AbiDataType.ADDRESS);
                Account a = states.get(HexBytes.fromBytes(addr));
                if (a.getContractHash() == null || a.getContractHash().length == 0)
                    throw new RuntimeException(HexBytes.fromBytes(addr) + " is not a contract account");
                byte[] data = RLPCodec.encode(a.getContractABIs());
                return WBI.mallocBytes(getInstance(), data);
            }
            default:
                throw new RuntimeException("unexpected type " + type);
        }
    }

    public enum Type {
        HEADER_PARENT_HASH,
        HEADER_CREATED_AT,
        HEADER_HEIGHT,
        TX_TYPE,
        TX_CREATED_AT,
        TX_NONCE,
        TX_ORIGIN,
        TX_GAS_PRICE,
        TX_AMOUNT,
        TX_TO,
        TX_SIGNATURE,
        TX_HASH,
        CONTRACT_ADDRESS,
        CONTRACT_NONCE,
        CONTRACT_CREATED_BY,
        ACCOUNT_NONCE,
        ACCOUNT_BALANCE,
        MSG_SENDER,
        MSG_AMOUNT,
        CONTRACT_CODE,
        CONTRACT_ABI
    }

}
