package org.tdf.sunflower.vm;

import lombok.Getter;
import lombok.NoArgsConstructor;
import org.tdf.common.types.Uint256;
import org.tdf.common.util.ByteUtil;
import org.tdf.common.util.HashUtil;
import org.tdf.common.util.HexBytes;
import org.tdf.lotusvm.ModuleInstance;
import org.tdf.lotusvm.types.Module;
import org.tdf.rlp.RLPList;
import org.tdf.sunflower.state.Account;
import org.tdf.sunflower.state.Bios;
import org.tdf.sunflower.state.PreBuiltContract;
import org.tdf.sunflower.types.CryptoContext;
import org.tdf.sunflower.types.Event;
import org.tdf.sunflower.types.TransactionResult;
import org.tdf.sunflower.vm.abi.Abi;
import org.tdf.sunflower.vm.abi.WbiType;
import org.tdf.sunflower.vm.hosts.*;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@NoArgsConstructor
public class VMExecutor {
    public VMExecutor(Backend backend, CallData callData, Limit limit, int depth) {
        this.backend = backend;
        this.callData = callData;
        this.limit = limit;
        this.depth = depth;
    }

    @Getter
    private Backend backend;
    @Getter
    private CallData callData;


    // gas limit hook
    private Limit limit;
    // call depth
    private int depth;


    public VMExecutor clone() {
        if (depth == 63)
            throw new RuntimeException("vm call depth overflow");
        return new VMExecutor(backend, callData.clone(), limit, depth + 1);
    }

    public static void assertContractAddress(HexBytes address) {
        if (address.size() != Account.ADDRESS_SIZE)
            throw new RuntimeException("invalid address size " + address.size());

        // address starts with 18 zero is reversed
        for (int i = 0; i < 18; i++) {
            if (address.get(i) != 0)
                return;
        }
        throw new RuntimeException("cannot call reversed address " + address);
    }


    public TransactionResult execute() {

        // 1. increase sender nonce
        long n = backend.getNonce(callData.getOrigin());
        if (!backend.isStatic() && n + 1 != callData.getTxNonce() && callData.getCallType() != CallType.COINBASE)
            throw new RuntimeException("invalid nonce");


        if (callData.getCallType() == CallType.CALL) {
            backend.setNonce(callData.getOrigin(), n + 1);
            // contract deploy nonce will increase internally
        }


        // 2. set initial gas by payload size
        limit.setInitialGas(backend.getInitialGas(callData.getData().size()));
        byte[] result = executeInternal();

        // 3. calculate fee and
        Uint256 fee = Uint256.of(limit.getGas()).mul(callData.getGasPrice());
        backend.subBalance(callData.getOrigin(), fee);


        List<Event> events = new ArrayList<>();

        for (Map.Entry<HexBytes, List<Map.Entry<String, RLPList>>> entries : backend.getEvents().entrySet()) {
            for (Map.Entry<String, RLPList> entry : entries.getValue()) {
                events.add(new Event(entry.getKey(), entry.getValue()));
            }
        }

        return new TransactionResult(limit.getGas(), result, events, fee);
    }

    public byte[] executeInternal() {
        switch (callData.getCallType()) {
            case COINBASE: {
                backend.addBalance(callData.getTxTo(), callData.getTxValue());
                for (Bios bios : backend.getBios().values()) {
                    return bios.call(backend, callData);
                }
                return ByteUtil.EMPTY_BYTE_ARRAY;
            }
            case CALL:
            case CREATE: {
                // is prebuilt
                if (backend.getPreBuiltContracts().containsKey(callData.getTo())) {
                    backend.addBalance(callData.getTo(), callData.getValue());
                    backend.subBalance(callData.getCaller(), callData.getValue());
                    PreBuiltContract updater = backend.getPreBuiltContracts().get(callData.getTo());
                    return updater.call(backend, callData);
                }

                byte[] code;
                byte[] data;
                HexBytes contractAddress;
                boolean create = callData.getCallType() == CallType.CREATE;

                // if this is contract deploy, should create contract account
                if (create) {
                    // increase sender nonce
                    long n = backend.getNonce(callData.getCaller());
                    backend.setNonce(callData.getCaller(), n + 1);
                    Module tmpModule = new Module(callData.getData().getBytes());
                    code = WBI.dropInit(callData.getData().getBytes());
                    data = WBI.extractInitData(tmpModule);

                    // increase nonce here to avoid conflicts
                    contractAddress = HexBytes.fromBytes(
                            HashUtil.calcNewAddr(
                            callData.getCaller().getBytes(), ByteUtil.longToBytesNoLeadZeroes(backend.getNonce(callData.getCaller())))
                    )
                    ;
                    callData.setTo(contractAddress);
                    backend.setCode(contractAddress, HexBytes.fromBytes(code));
                    backend.setContractCreatedBy(contractAddress, callData.getCaller());
                } else {
                    contractAddress = callData.getTo();
                    code = backend.getCode(contractAddress).getBytes();
                    data = callData.getData().getBytes();
                }

                // is wasm call
                backend.addBalance(contractAddress, callData.getValue());
                backend.subBalance(callData.getCaller(), callData.getValue());

                if (code.length == 0)
                    throw new RuntimeException("contract not found or is not a ccontract address");

                DBFunctions dbFunctions = new DBFunctions(backend, callData.getTo());

                Hosts hosts = new Hosts()
                        .withTransfer(
                                backend,
                                callData.getTo()
                        )
                        .withReflect(new Reflect(this))
                        .withContext(new ContextHost(backend, callData))
                        .withDB(dbFunctions)
                        .withEvent(backend, callData.getTo());

                Module m = new Module(code);
                Abi abi = m.getCustomSections()
                        .stream().filter(x -> x.getName().equals("__abi"))
                        .findFirst()
                        .map(x -> Abi.fromJson(new String(x.getData(), StandardCharsets.UTF_8)))
                        .get();

                ModuleInstance instance = ModuleInstance
                        .builder()
                        .module(m)
                        .hooks(Collections.singleton(limit))
                        .hostFunctions(hosts.getAll())
                        .build();

                WBI.InjectResult r = WBI.inject(create, abi, instance, HexBytes.fromBytes(data));

                RLPList ret = RLPList.createEmpty();

                if (!r.isExecutable())
                    return ByteUtil.EMPTY_BYTE_ARRAY;

                // put parameters will not consume steps
                long[] rets = instance.execute(r.getFunction(), r.getPointers());

                Object result = null;
                // extract result
                if (rets.length > 0) {
                    List<Abi.Entry.Param> outputs;

                    if (create) {
                        outputs = abi.findConstructor().outputs;
                    } else {
                        outputs = abi.findFunction(x -> x.name.equals(r.getFunction())).outputs;
                    }

                    // outputs
                    int outType = ByteBuffer.wrap(
                            HashUtil.sha3(outputs.get(0).type.getName().getBytes(StandardCharsets.US_ASCII)),
                            0,
                            4
                    ).order(ByteOrder.BIG_ENDIAN).getInt();

                    switch (outType) {
                        case WbiType.BYTES:
                        case WbiType.STRING:
                        case WbiType.ADDRESS:
                        case WbiType.UINT_256: {
                            result = WBI.peek(instance, (int) rets[0], outType);
                            break;
                        }
                        default: {
                            result = rets[0];
                        }
                    }
                }


                return ByteUtil.EMPTY_BYTE_ARRAY;
            }
            default:
                throw new UnsupportedOperationException();
        }
    }
}
