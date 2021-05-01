package org.tdf.sunflower.vm.hosts;

import org.tdf.common.util.HexBytes;
import org.tdf.lotusvm.runtime.HostFunction;
import org.tdf.lotusvm.types.FunctionType;
import org.tdf.lotusvm.types.ValueType;
import org.tdf.sunflower.vm.Backend;
import org.tdf.sunflower.vm.CallData;
import org.tdf.sunflower.vm.WBI;
import org.tdf.sunflower.vm.abi.WbiType;

import java.util.Arrays;
import java.util.Collections;


public class ContextHost extends HostFunction {
    private final Backend backend;
    private final CallData callData;
    public static final FunctionType FUNCTION_TYPE = new FunctionType(Arrays.asList(ValueType.I64, ValueType.I64),
            Collections.singletonList(ValueType.I64));

    public ContextHost(
            Backend backend,
            CallData callData
    ) {
        super("_context", FUNCTION_TYPE);
        this.backend = backend;
        this.callData = callData;
    }

    @Override
    public long execute(long... parameters) {
        Type type = Type.values()[(int) parameters[0]];

        switch (type) {
            case HEADER_PARENT_HASH: {
                return WBI
                        .mallocBytes(getInstance(), backend.getParentHash());

            }
            case HEADER_CREATED_AT: {
                return backend.getHeaderCreatedAt();
            }

            case HEADER_HEIGHT: {
                return backend.getHeight();
            }
            case TX_NONCE: {
                return callData.getTxNonce();
            }
            case TX_ORIGIN: {
                return WBI.mallocAddress(getInstance(), callData.getOrigin());
            }
            case TX_GAS_PRICE: {
                return WBI.malloc(getInstance(), callData.getGasPrice());
            }
            case TX_AMOUNT: {
                return WBI.malloc(getInstance(), callData.getTxValue());
            }
            case TX_TO: {
                return WBI.mallocAddress(getInstance(), callData.getTxTo());
            }
            case TX_HASH: {
                return WBI.mallocBytes(getInstance(), callData.getTxHash());
            }
            case CONTRACT_ADDRESS: {
                return WBI
                        .mallocAddress(getInstance(), callData.getTo());
            }
            case CONTRACT_NONCE: {
                return backend.getNonce(callData.getTo());
            }
            case CONTRACT_CREATED_BY: {
                return WBI.mallocAddress(getInstance(), backend.getContractCreatedBy(callData.getTo()));
            }
            case ACCOUNT_NONCE: {
                byte[] addr = (byte[]) WBI
                        .peek(getInstance(), (int) parameters[1], WbiType.ADDRESS);
                return backend.getNonce(HexBytes.fromBytes(addr));
            }
            case ACCOUNT_BALANCE: {
                byte[] addr = (byte[]) WBI
                        .peek(getInstance(), (int) parameters[1], WbiType.ADDRESS);
                return WBI.malloc(getInstance(), backend.getBalance(HexBytes.fromBytes(addr)));
            }
            case MSG_SENDER: {
                return WBI.mallocAddress(getInstance(), callData.getCaller());
            }
            case MSG_AMOUNT: {
                return
                        WBI.malloc(getInstance(), callData.getValue());
            }
            case CONTRACT_CODE: {
                byte[] addr = (byte[]) WBI
                        .peek(getInstance(), (int) parameters[1], WbiType.ADDRESS);
                return WBI.mallocBytes(getInstance(), backend.getCode(HexBytes.fromBytes(addr)));
            }
            default:
                throw new RuntimeException("unexpected type " + type);
        }
    }

    public enum Type {
        HEADER_PARENT_HASH,
        HEADER_CREATED_AT,
        HEADER_HEIGHT,
        TX_NONCE,
        TX_ORIGIN,
        TX_GAS_PRICE,
        TX_AMOUNT,
        TX_TO,
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
