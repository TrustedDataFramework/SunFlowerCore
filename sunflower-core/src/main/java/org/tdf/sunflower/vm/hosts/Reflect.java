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
    public long execute(long... longs) {
        Type t = Type.values()[(int) longs[0]];
        switch (t) {
            case CALL: {
                byte[] addr = (byte[]) WBI.peek(getInstance(), (int) longs[1], AbiDataType.ADDRESS);
                String method = (String) WBI.peek(getInstance(), (int) longs[2], AbiDataType.STRING);
                if ("init".equals(method))
                    throw new RuntimeException("cannot call constructor");
                byte[] parameters = (byte[]) WBI.peek(getInstance(), (int) longs[3], AbiDataType.BYTES);
                Parameters params = RLPCodec.decode(parameters, Parameters.class);
                Uint256 amount = (Uint256) WBI.peek(getInstance(), (int) longs[4], AbiDataType.U256);
                ContractCall forked = parent.fork();
                RLPList result = forked.call(HexBytes.fromBytes(addr), method, params, amount, false, null).getReturns();
                if(result.isEmpty())
                    return 0;

                RLPItem it = result.get(0).asRLPItem();
                switch (AbiDataType.values()[params.getReturnType()[0]]){
                    case F64:
                    case I64:
                    case BOOL:
                    case U64:
                        return it.asLong();
                    case U256:{
                        return WBI.malloc(getInstance(), it.as(Uint256.class));
                    }
                    case STRING:{
                        return WBI.malloc(getInstance(), it.asString());
                    }
                    case BYTES:{
                        return WBI.mallocBytes(getInstance(), it.asBytes());
                    }
                    case ADDRESS:{
                        return WBI.mallocAddress(getInstance(), it.asBytes());
                    }
                }
                break;
            }
            case CREATE:
                if (this.readonly)
                    throw new RuntimeException("cannot create contract here");
                byte[] binary = (byte[]) WBI.peek(getInstance(), (int) longs[1], AbiDataType.BYTES);
                byte[] parameters = (byte[]) WBI.peek(getInstance(), (int) longs[3], AbiDataType.BYTES);
                byte[] abi = (byte[]) WBI.peek(getInstance(), (int) longs[5], AbiDataType.BYTES);
                Uint256 amount = (Uint256) WBI.peek(getInstance(), (int) longs[4], AbiDataType.U256);
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
                return WBI.mallocAddress(getInstance(), data);
        }
        throw new RuntimeException("unreachable");
    }

    enum Type {
        CALL, // call and put into memory
        CREATE // create
    }
}
