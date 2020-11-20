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
    public long[] execute(long... parameters) {
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
                long r = WasmBlockChainInterface
                        .mallocBytes(getInstance(), context.getHeader().getHashPrev().getBytes());
                return new long[]{r};
            }
            case HEADER_CREATED_AT: {
                long r = context.getHeader().getCreatedAt();
                return new long[]{r};
            }
            case HEADER_HEIGHT: {
                long r = context.getHeader().getHeight();
                return new long[]{r};
            }
            case TX_TYPE: {
                long r = context.getTransaction().getType();
                return new long[]{r};
            }
            case TX_CREATED_AT: {
                long r = context.getTransaction().getCreatedAt();
                return new long[]{r};
            }
            case TX_NONCE: {
                long r = context.getTransaction().getNonce();
                return new long[]{r};
            }
            case TX_ORIGIN: {
                long r = WasmBlockChainInterface.mallocAddress(getInstance(), context.getTransaction().getFromAddress().getBytes());
                return new long[]{r};
            }
            case TX_GAS_PRICE: {
                long r = WasmBlockChainInterface.malloc(getInstance(), context.getTransaction().getGasPrice());
                return new long[]{r};
            }
            case TX_AMOUNT: {
                long r = WasmBlockChainInterface.malloc(getInstance(), context.getTransaction().getAmount());
                return new long[]{r};
            }
            case TX_TO: {
                long r = WasmBlockChainInterface.mallocAddress(getInstance(), context.getTransaction().getTo().getBytes());
                return new long[]{r};
            }
            case TX_SIGNATURE: {
                long r = WasmBlockChainInterface.mallocBytes(getInstance(), context.getTransaction().getSignature().getBytes());
                return new long[]{r};
            }
            case TX_HASH: {
                long r = WasmBlockChainInterface.mallocBytes(getInstance(), context.getTransaction().getHash().getBytes());
                return new long[]{r};
            }
            case CONTRACT_ADDRESS: {
                long r = WasmBlockChainInterface
                        .mallocAddress(getInstance(), context.getContractAccount().getAddress().getBytes());

                return new long[]{r};
            }
            case CONTRACT_NONCE: {
                long r = context.getContractAccount().getNonce();
                return new long[]{r};
            }
            case CONTRACT_CREATED_BY: {
                long r = WasmBlockChainInterface.mallocAddress(getInstance(), context.getContractAccount().getCreatedBy().getBytes());
                return new long[]{r};
            }
            case ACCOUNT_NONCE: {
                byte[] addr = (byte[]) WasmBlockChainInterface
                        .peek(getInstance(), (int) parameters[1], AbiDataType.ADDRESS);
                Account a = states.get(HexBytes.fromBytes(addr));
                long r = a.getNonce();
                return new long[]{r};
            }
            case ACCOUNT_BALANCE: {
                byte[] addr = (byte[]) WasmBlockChainInterface
                        .peek(getInstance(), (int) parameters[1], AbiDataType.ADDRESS);
                Account a = states.get(HexBytes.fromBytes(addr));
                long r = WasmBlockChainInterface.malloc(getInstance(), a.getBalance());
                return new long[]{r};
            }
            case MSG_SENDER: {
                long r = WasmBlockChainInterface.mallocAddress(getInstance(), context.getMsgSender().getBytes());
                return new long[]{r};
            }
            case MSG_AMOUNT: {
                long r =
                        WasmBlockChainInterface.malloc(getInstance(), context.getAmount());
                return new long[]{r};
            }
            case CONTRACT_CODE: {
                byte[] addr = (byte[]) WasmBlockChainInterface
                        .peek(getInstance(), (int) parameters[1], AbiDataType.ADDRESS);
                Account a = states.get(HexBytes.fromBytes(addr));
                if (a.getContractHash() == null || a.getContractHash().length == 0)
                    throw new RuntimeException(HexBytes.fromBytes(addr) + " is not a contract account");
                byte[] code = this.contractCodeStore.get(a.getContractHash()).get();
                long r = WasmBlockChainInterface.mallocBytes(getInstance(), code);
                return new long[]{r};
            }
            case CONTRACT_ABI: {
                byte[] addr = (byte[]) WasmBlockChainInterface
                        .peek(getInstance(), (int) parameters[1], AbiDataType.ADDRESS);
                Account a = states.get(HexBytes.fromBytes(addr));
                if (a.getContractHash() == null || a.getContractHash().length == 0)
                    throw new RuntimeException(HexBytes.fromBytes(addr) + " is not a contract account");
                byte[] data = RLPCodec.encode(a.getContractABIs());
                long r = WasmBlockChainInterface.mallocBytes(getInstance(), data);
                return new long[]{r};
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
