package org.tdf.sunflower.vm.hosts;

import org.tdf.common.store.Store;
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

public class ContextHost extends HostFunction {
    public enum Type{
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
        ARGUMENTS_METHOD,
        ARGUMENTS_PARAMETERS,
        ACCOUNT_NONCE,
        ACCOUNT_BALANCE,
        MSG_SENDER,
        MSG_AMOUNT,
        CONTRACT_CODE
    }

    private Context context;
    private Map<HexBytes, Account> states;
    private Store<byte[], byte[]> contractCodeStore;


    public ContextHost(Context context, Map<HexBytes, Account> states, Store<byte[], byte[]> contractCodeStore) {
        setName("_context");
        setType(
                new FunctionType(Arrays.asList(ValueType.I64, ValueType.I64, ValueType.I64, ValueType.I64, ValueType.I64),
                        Collections.singletonList(ValueType.I64))
        );
        this.context = context;
        this.states = states;
        this.contractCodeStore = contractCodeStore;
    }

    @Override
    public long[] execute(long... parameters) {
        Type type = Type.values()[(int) parameters[0]];
        long ret = 0;
        boolean isPut = true;
        byte[] data = null;
        switch (type) {
            case HEADER_PARENT_HASH:{
                data = context.getHeader().getHashPrev().getBytes();
                ret = data.length;
                break;
            }
            case HEADER_CREATED_AT:{
                isPut = false;
                ret = context.getHeader().getCreatedAt();
                break;
            }
            case HEADER_HEIGHT:{
                isPut = false;
                ret = context.getHeader().getHeight();
                break;
            }
            case TX_TYPE:{
                isPut = false;
                ret = context.getTransaction().getType();
                break;
            }
            case TX_CREATED_AT:{
                isPut = false;
                ret = context.getTransaction().getCreatedAt();
                break;
            }
            case TX_NONCE:{
                isPut = false;
                ret = context.getTransaction().getNonce();
                break;
            }
            case TX_ORIGIN:{
                data = context.getTransaction().getFromAddress().getBytes();
                ret = data.length;
                break;
            }
            case TX_GAS_PRICE:{
                isPut = false;
                ret = context.getTransaction().getGasPrice();
                break;
            }
            case TX_AMOUNT:{
                isPut = false;
                ret = context.getTransaction().getAmount();
                break;
            }
            case TX_TO:{
                data = context.getTransaction().getTo().getBytes();
                ret = data.length;
                break;
            }
            case TX_SIGNATURE:{
                data = context.getTransaction().getSignature().getBytes();
                ret = data.length;
                break;
            }
            case TX_HASH:{
                data = context.getTransaction().getHash().getBytes();
                ret = data.length;
                break;
            }
            case CONTRACT_ADDRESS:{
                data = context.getContractAccount().getAddress().getBytes();
                ret = data.length;
                break;
            }
            case CONTRACT_NONCE:{
                isPut = false;
                ret = context.getContractAccount().getNonce();
                break;
            }
            case CONTRACT_CREATED_BY:{
                data = context.getContractAccount().getCreatedBy().getBytes();
                ret = data.length;
                break;
            }
            case ARGUMENTS_METHOD:{
                data = context.getMethod().getBytes(StandardCharsets.US_ASCII);
                ret = data.length;
                break;
            }
            case ARGUMENTS_PARAMETERS:{
                data = context.getParameters();
                ret = data.length;
                break;
            }
            case ACCOUNT_NONCE:{
                isPut = false;
                byte[] addr = loadMemory((int) parameters[1], (int) parameters[2]);
                Account a = states.get(HexBytes.fromBytes(addr));
                ret = a.getNonce();
                break;
            }
            case ACCOUNT_BALANCE:{
                isPut = false;
                byte[] addr = loadMemory((int) parameters[1], (int) parameters[2]);
                Account a = states.get(HexBytes.fromBytes(addr));
                ret = a.getBalance();
                break;
            }
            case MSG_SENDER:{
                data = context.getMsgSender().getBytes();
                ret = data.length;
                break;
            }
            case MSG_AMOUNT:{
                isPut = false;
                ret = context.getAmount();
                break;
            }
            case CONTRACT_CODE:{
                byte[] addr = loadMemory((int) parameters[1], (int) parameters[2]);
                Account a = states.get(HexBytes.fromBytes(addr));
                if(a.getContractHash() == null || a.getContractHash().length == 0)
                    throw new RuntimeException(HexBytes.fromBytes(addr) + " is not a contract account");
                data = this.contractCodeStore.get(a.getContractHash()).get();
                ret = data.length;
                break;
            }
            default:
                throw new RuntimeException("unexpected type " + type);
        }

        if(isPut && parameters[2] != 0){
            putMemory((int) parameters[1], data);
        }
        return new long[]{ret};
    }

}
