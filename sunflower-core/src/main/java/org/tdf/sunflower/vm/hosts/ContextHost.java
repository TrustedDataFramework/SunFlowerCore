package org.tdf.sunflower.vm.hosts;

import org.tdf.common.store.Store;
import org.tdf.common.trie.Trie;
import org.tdf.common.util.HexBytes;
import org.tdf.lotusvm.runtime.HostFunction;
import org.tdf.lotusvm.types.FunctionType;
import org.tdf.lotusvm.types.ValueType;
import org.tdf.sunflower.state.Account;
import org.tdf.sunflower.vm.abi.Context;

import java.nio.charset.StandardCharsets;
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
                new FunctionType(Arrays.asList(ValueType.I64, ValueType.I64, ValueType.I64, ValueType.I64, ValueType.I64),
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
        long ret = 0;
        boolean isPut = parameters[2] != 0;
        byte[] data = null;
        long offset = parameters[1];
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
                data = context.getHeader().getHashPrev().getBytes();
                ret = data.length;
                break;
            }
            case HEADER_CREATED_AT: {
                isPut = false;
                ret = context.getHeader().getCreatedAt();
                break;
            }
            case HEADER_HEIGHT: {
                isPut = false;
                ret = context.getHeader().getHeight();
                break;
            }
            case TX_TYPE: {
                isPut = false;
                ret = context.getTransaction().getType();
                break;
            }
            case TX_CREATED_AT: {
                isPut = false;
                ret = context.getTransaction().getCreatedAt();
                break;
            }
            case TX_NONCE: {
                isPut = false;
                ret = context.getTransaction().getNonce();
                break;
            }
            case TX_ORIGIN: {
                data = context.getTransaction().getFromAddress().getBytes();
                ret = data.length;
                break;
            }
            case TX_GAS_PRICE: {
                data = context.getTransaction().getGasPrice().getNoLeadZeroesData();
                ret = data.length;
                break;
            }
            case TX_AMOUNT: {
                data = context.getTransaction().getAmount().getNoLeadZeroesData();
                ret = data.length;
                break;
            }
            case TX_TO: {
                data = context.getTransaction().getTo().getBytes();
                ret = data.length;
                break;
            }
            case TX_SIGNATURE: {
                data = context.getTransaction().getSignature().getBytes();
                ret = data.length;
                break;
            }
            case TX_HASH: {
                data = context.getTransaction().getHash().getBytes();
                ret = data.length;
                break;
            }
            case CONTRACT_ADDRESS: {
                data = context.getContractAccount().getAddress().getBytes();
                ret = data.length;
                break;
            }
            case CONTRACT_NONCE: {
                isPut = false;
                ret = context.getContractAccount().getNonce();
                break;
            }
            case CONTRACT_CREATED_BY: {
                data = context.getContractAccount().getCreatedBy().getBytes();
                ret = data.length;
                break;
            }
            case ACCOUNT_NONCE: {
                isPut = false;
                byte[] addr = loadMemory((int) parameters[1], (int) parameters[2]);
                Account a = states.get(HexBytes.fromBytes(addr));
                ret = a.getNonce();
                break;
            }
            case ACCOUNT_BALANCE: {
                byte[] addr = loadMemory((int) parameters[1], (int) parameters[2]);
                Account a = states.get(HexBytes.fromBytes(addr));
                data = a.getBalance().getNoLeadZeroesData();
                offset = parameters[3];
                isPut = parameters[4] != 0;
                ret = data.length;
                break;
            }
            case MSG_SENDER: {
                data = context.getMsgSender().getBytes();
                ret = data.length;
                break;
            }
            case MSG_AMOUNT: {
                data = context.getAmount().getNoLeadZeroesData();
                ret = data.length;
                break;
            }
            case CONTRACT_CODE: {
                byte[] addr = loadMemory((int) parameters[1], (int) parameters[2]);
                Account a = states.get(HexBytes.fromBytes(addr));
                if (a.getContractHash() == null || a.getContractHash().length == 0)
                    throw new RuntimeException(HexBytes.fromBytes(addr) + " is not a contract account");
                data = this.contractCodeStore.get(a.getContractHash()).get();
                ret = data.length;
                isPut = parameters[4] != 0;
                offset = parameters[3];
                break;
            }
            case CONTRACT_ABI: {
                byte[] addr = loadMemory((int) parameters[1], (int) parameters[2]);
                Account a = states.get(HexBytes.fromBytes(addr));
                if (a.getContractHash() == null || a.getContractHash().length == 0)
                    throw new RuntimeException(HexBytes.fromBytes(addr) + " is not a contract account");
                data = this.storageTrieSupplier.apply(a.getStorageRoot()).get("__abi".getBytes(StandardCharsets.UTF_8)).get();
                ret = data.length;
                isPut = parameters[4] != 0;
                offset = parameters[3];
                break;
            }
            default:
                throw new RuntimeException("unexpected type " + type);
        }

        if (isPut) {
            putMemory((int) offset, data);
        }
        return new long[]{ret};
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
