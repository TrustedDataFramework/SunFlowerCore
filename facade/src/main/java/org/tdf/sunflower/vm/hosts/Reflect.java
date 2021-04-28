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
import org.tdf.sunflower.types.Transaction;
import org.tdf.sunflower.vm.WBI;
import org.tdf.sunflower.vm.abi.WbiType;
import org.tdf.sunflower.vm.ContractABI;
import org.tdf.sunflower.vm.abi.ContractCallPayload;
import org.tdf.sunflower.vm.abi.ContractDeployPayload;

import java.util.Arrays;
import java.util.Collections;

public class Reflect extends HostFunction {
    private final org.tdf.sunflower.vm.VMExecutor VMExecutor;
    public static final FunctionType FUNCTION_TYPE = new FunctionType(
            // offset, length, offset
            Arrays.asList(
                    ValueType.I64, ValueType.I64, ValueType.I64, ValueType.I64,
                    ValueType.I64, ValueType.I64
            ),
            Collections.singletonList(ValueType.I64)
    );

    public Reflect(org.tdf.sunflower.vm.VMExecutor VMExecutor) {
        super("_reflect", FUNCTION_TYPE);
        this.VMExecutor = VMExecutor;
    }

    @Override
    public long execute(long... longs) {
        Type t = Type.values()[(int) longs[0]];
        byte[] payload = null;
        Uint256 amount = null;
        org.tdf.sunflower.vm.VMExecutor forked = VMExecutor.clone();
        Parameters params = null;
        switch (t) {
            case CALL: {
                byte[] addr = (byte[]) WBI.peek(getInstance(), (int) longs[1], WbiType.ADDRESS);
                forked.getCallData().setTo(HexBytes.fromBytes(addr));
                String method = (String) WBI.peek(getInstance(), (int) longs[2], WbiType.STRING);

                if ("init".equals(method))
                    throw new RuntimeException("cannot call constructor");

                byte[] parameters = (byte[]) WBI.peek(getInstance(), (int) longs[3], WbiType.BYTES);
                params = RLPCodec.decode(parameters, Parameters.class);
                amount = (Uint256) WBI.peek(getInstance(), (int) longs[4], WbiType.UINT_256);
                payload = RLPCodec.encode(new ContractCallPayload(method, params));
                forked.getCallData().setCallType(Transaction.Type.CONTRACT_CALL.code);
                break;
            }
            case CREATE: {
                forked.getCallData().setTo(HexBytes.empty());
                byte[] binary = (byte[]) WBI.peek(getInstance(), (int) longs[1], WbiType.BYTES);
                byte[] parameters = (byte[]) WBI.peek(getInstance(), (int) longs[3], WbiType.BYTES);
                byte[] abi = (byte[]) WBI.peek(getInstance(), (int) longs[5], WbiType.BYTES);
                params = RLPCodec.decode(parameters, Parameters.class);
                payload = RLPCodec.encode(new ContractDeployPayload(
                        binary, params,
                        Arrays.asList(RLPCodec.decode(abi, ContractABI[].class))
                ));
                amount = (Uint256) WBI.peek(getInstance(), (int) longs[4], WbiType.UINT_256);
                forked.getCallData().setCallType(Transaction.Type.CONTRACT_DEPLOY.code);
                break;
            }
            default:
                throw new RuntimeException("unexpected");
        }

        forked.getCallData().setAmount(amount);
        forked.getCallData().setPayload(HexBytes.fromBytes(payload));
        forked.getCallData().setCaller(VMExecutor.getCallData().getTo());
        RLPList result = forked.executeInternal();

        if (result.isEmpty())
            return 0;

        RLPItem it = result.get(0).asRLPItem();
        throw new RuntimeException("unexpected");
    }


    enum Type {
        CALL, // call and put into memory
        CREATE // create
    }
}
