package org.tdf.sunflower.vm.hosts;

import org.tdf.common.types.Parameters;
import org.tdf.common.types.Uint256;
import org.tdf.common.util.HexBytes;
import org.tdf.lotusvm.runtime.HostFunction;
import org.tdf.lotusvm.types.FunctionType;
import org.tdf.lotusvm.types.ValueType;
import org.tdf.rlp.RLPCodec;
import org.tdf.rlp.RLPItem;
import org.tdf.rlp.RLPList;
import org.tdf.sunflower.types.TransactionResult;
import org.tdf.sunflower.vm.abi.AbiDataType;
import org.tdf.sunflower.vm.abi.ContractABI;
import org.tdf.sunflower.vm.abi.ContractCall;

import java.util.Arrays;
import java.util.Collections;

public class Reflect extends HostFunction {
    private final ContractCall parent;
    private byte[] result;
    private boolean readonly;

    public Reflect(ContractCall parent, boolean readonly) {
        this.parent = parent;
        setType(new FunctionType(
                // offset, length, offset
                Arrays.asList(
                        ValueType.I64, ValueType.I64, ValueType.I64, ValueType.I64,
                        ValueType.I64, ValueType.I64
                ),
                Collections.singletonList(ValueType.I64)
        ));
        setName("_reflect");
        this.readonly = readonly;
    }

    @Override
    public long[] execute(long... longs) {
        Type t = Type.values()[(int) longs[0]];
        switch (t) {
            case CALL: {
                byte[] addr = (byte[]) WasmBlockChainInterface.mpeek(getInstance(), (int) longs[1], AbiDataType.ADDRESS);
                String method = (String) WasmBlockChainInterface.mpeek(getInstance(), (int) longs[2], AbiDataType.STRING);
                if ("init".equals(method))
                    throw new RuntimeException("cannot call constructor");
                byte[] parameters = (byte[]) WasmBlockChainInterface.mpeek(getInstance(), (int) longs[3], AbiDataType.BYTES);
                Parameters params = RLPCodec.decode(parameters, Parameters.class);
                Uint256 amount = (Uint256) WasmBlockChainInterface.mpeek(getInstance(), (int) longs[4], AbiDataType.U256);
                ContractCall forked = parent.fork();
                RLPList result = forked.call(HexBytes.fromBytes(addr), method, params, amount, false, null).getReturns();
                if(result.isEmpty())
                    return new long[1];

                RLPItem it = result.get(0).asRLPItem();
                switch (AbiDataType.values()[params.getReturnType()[0]]){
                    case F64:
                    case I64:
                    case BOOL:
                    case U64:
                        return new long[]{it.asLong()};
                    case U256:{
                        long r = WasmBlockChainInterface.malloc(getInstance(), it.as(Uint256.class));
                        return new long[]{r};
                    }
                    case STRING:{
                        long r = WasmBlockChainInterface.malloc(getInstance(), it.asString());
                        return new long[]{r};
                    }
                    case BYTES:{
                        long r = WasmBlockChainInterface.mallocBytes(getInstance(), it.asBytes());
                        return new long[]{r};
                    }
                    case ADDRESS:{
                        long r = WasmBlockChainInterface.mallocAddress(getInstance(), it.asBytes());
                        return new long[]{r};
                    }
                }
                break;
            }
            case CREATE:
                if (this.readonly)
                    throw new RuntimeException("cannot create contract here");
                byte[] binary = (byte[]) WasmBlockChainInterface.mpeek(getInstance(), (int) longs[1], AbiDataType.BYTES);
                byte[] parameters = (byte[]) WasmBlockChainInterface.mpeek(getInstance(), (int) longs[3], AbiDataType.BYTES);
                byte[] abi = (byte[]) WasmBlockChainInterface.mpeek(getInstance(), (int) longs[5], AbiDataType.BYTES);
                Uint256 amount = (Uint256) WasmBlockChainInterface.mpeek(getInstance(), (int) longs[4], AbiDataType.U256);
                ContractCall forked = parent.fork();
                byte[] data = forked
                        .call(HexBytes.fromBytes(binary),
                                "init",
                                RLPCodec.decode(parameters, Parameters.class),
                                amount,
                                true,
                                Arrays.asList(RLPCodec.decode(abi, ContractABI[].class))
                        )
                        .getReturns().get(0).asBytes();
                long r = WasmBlockChainInterface.mallocAddress(getInstance(), data);
                return new long[]{r};
        }
        throw new RuntimeException("unreachable");
    }

    enum Type {
        CALL, // call and put into memory
        CREATE // create
    }
}
