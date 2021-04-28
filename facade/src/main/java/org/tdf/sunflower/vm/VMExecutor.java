package org.tdf.sunflower.vm;

import lombok.Getter;
import lombok.NoArgsConstructor;
import org.tdf.common.types.Parameters;
import org.tdf.common.types.Uint256;
import org.tdf.common.util.HexBytes;
import org.tdf.lotusvm.ModuleInstance;
import org.tdf.rlp.RLPCodec;
import org.tdf.rlp.RLPList;
import org.tdf.sunflower.state.Account;
import org.tdf.sunflower.state.Bios;
import org.tdf.sunflower.state.PreBuiltContract;
import org.tdf.sunflower.types.Event;
import org.tdf.sunflower.types.Transaction;
import org.tdf.sunflower.types.TransactionResult;
import org.tdf.sunflower.vm.abi.WbiType;
import org.tdf.sunflower.vm.abi.ContractCallPayload;
import org.tdf.sunflower.vm.abi.ContractDeployPayload;
import org.tdf.sunflower.vm.hosts.*;

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
        if(depth == 63)
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

    public static int allocString(ModuleInstance moduleInstance, String s) {
        return WBI.malloc(moduleInstance, s);
    }

    public static int allocBytes(ModuleInstance moduleInstance, byte[] buf) {
        return WBI.mallocBytes(moduleInstance, buf);
    }

    public static int allocAddress(ModuleInstance moduleInstance, byte[] addr) {
        return WBI.mallocAddress(moduleInstance, addr);
    }

    public static int allocU256(ModuleInstance moduleInstance, Uint256 u) {
        return WBI.malloc(moduleInstance, u);
    }

    static Object getResult(ModuleInstance module, long offset, WbiType type) {
        return null;
    }

    private long[] putParameters(ModuleInstance module, Parameters params) {
        long[] ret = new long[params.getTypes().length];
        for (int i = 0; i < ret.length; i++) {
            return new long[0];
        }
        return ret;
    }

    public TransactionResult execute() {
        Transaction.Type t = Transaction.Type.values()[callData.getCallType()];

        // 1. increase sender nonce
        long n = backend.getNonce(callData.getOrigin());
        if(!backend.isStatic() && n + 1 != callData.getTxNonce() && t != Transaction.Type.COIN_BASE)
            throw new RuntimeException("invalid nonce");


        switch (t) {
            case TRANSFER:
            case CONTRACT_CALL:{
                backend.setNonce(callData.getOrigin(), n + 1);
                break;
            }
            // contract deploy nonce will increase internally
        }


        // 2. set initial gas by payload size
        limit.setInitialGas(backend.getInitialGas(callData.getPayload().size()));

        RLPList result = executeInternal();

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

    public RLPList executeInternal() {
        Transaction.Type t = Transaction.Type.values()[callData.getCallType()];

        switch (t) {
            case COIN_BASE: {
                backend.addBalance(callData.getTxTo(), callData.getTxAmount());
                for (Bios bios : backend.getBios().values()) {
                    bios.update(backend);
                }
                return RLPList.createEmpty();
            }
            case TRANSFER: {
                // transfer balance
                limit.addGas(Transaction.TRANSFER_GAS);
                backend.addBalance(callData.getTo(), callData.getAmount());
                backend.subBalance(callData.getCaller(), callData.getAmount());
                return RLPList.createEmpty();
            }
            case CONTRACT_CALL:
            case CONTRACT_DEPLOY:{
                // is prebuilt
                if (backend.getPreBuiltContracts().containsKey(callData.getTo())) {
                    limit.addGas(Transaction.BUILTIN_CALL_GAS);
                    backend.addBalance(callData.getTo(), callData.getAmount());
                    backend.subBalance(callData.getCaller(), callData.getAmount());
                    PreBuiltContract updater = backend.getPreBuiltContracts().get(callData.getTo());
                    updater.update(backend, callData);
                    return RLPList.createEmpty();
                }

                Parameters parameters;
                String method;
                HexBytes contractAddress;

                // if this is contract deploy, should create contract account
                if (t == Transaction.Type.CONTRACT_DEPLOY) {
                    // increase sender nonce
                    long n = backend.getNonce(callData.getCaller());
                    backend.setNonce(callData.getCaller(), n + 1);

                    ContractDeployPayload deployPayload =
                            RLPCodec.decode(
                                    callData.getPayload().getBytes(),
                                    ContractDeployPayload.class
                            );
                    parameters = deployPayload.getParameters();
                    method = "init";
                    // increase nonce here to avoid conflicts
                    contractAddress = Transaction.createContractAddress(callData.getCaller(), backend.getNonce(callData.getCaller()));
                    callData.setTo(contractAddress);
                    backend.setCode(contractAddress, deployPayload.getBinary());
                    backend.setABI(contractAddress, deployPayload.getContractABIs());
                    backend.setContractCreatedBy(contractAddress, callData.getCaller());
                } else {
                    ContractCallPayload callPayload =
                            RLPCodec.decode(
                                    callData.getPayload().getBytes(), ContractCallPayload.class
                            );
                    parameters = callPayload.getParameters();
                    method = callPayload.getMethod();
                    if (method.equals("init"))
                        throw new RuntimeException("cannot call construct");
                    contractAddress = callData.getTo();
                }

                // is wasm call
                backend.addBalance(contractAddress, callData.getAmount());
                backend.subBalance(callData.getCaller(), callData.getAmount());
                byte[] contractCode = backend.getCode(contractAddress);

                if (contractCode.length == 0)
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

                ModuleInstance instance = ModuleInstance
                        .builder()
                        .binary(contractCode)
                        .hooks(Collections.singleton(limit))
                        .hostFunctions(hosts.getAll())
                        .build();


                RLPList ret = RLPList.createEmpty();

                if (method.equals("init") && !instance.containsExport("init"))
                    return ret;

                // put parameters will not consume steps
                long steps = limit.getSteps();
                long[] offsets = putParameters(instance, parameters);
                limit.setSteps(steps);

                long[] rets = instance.execute(method, offsets);

                return ret;
            }
            default:
                throw new UnsupportedOperationException();
        }
    }
}
