package org.tdf.sunflower.vm;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import org.tdf.common.types.Uint256;
import org.tdf.common.util.ByteUtil;
import org.tdf.common.util.HashUtil;
import org.tdf.common.util.HexBytes;
import org.tdf.lotusvm.ModuleInstance;
import org.tdf.lotusvm.runtime.Memory;
import org.tdf.lotusvm.runtime.UnsafeMemory;
import org.tdf.lotusvm.types.Module;
import org.tdf.sunflower.state.Address;
import org.tdf.sunflower.state.BuiltinContract;
import org.tdf.sunflower.types.VMResult;
import org.tdf.sunflower.vm.abi.Abi;
import org.tdf.sunflower.vm.abi.SolidityType;
import org.tdf.sunflower.vm.abi.WbiType;
import org.tdf.sunflower.vm.hosts.*;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@NoArgsConstructor
public class VMExecutor {
    public static final int MAX_FRAMES = 16384;
    public static final int MAX_STACK_SIZE = MAX_FRAMES * 64;
    public static final int MAX_LABELS = MAX_FRAMES * 64;
    public static final int MAX_CALL_DEPTH = 8;
    public static final Cache<HexBytes, Module> CACHE =
        CacheBuilder
            .newBuilder()
            .maximumSize(128)
            .build();

    public VMExecutor(Backend backend, CallData callData, StackResourcePool pool, long gasLimit) {
        this(backend, callData, pool, new Limit(gasLimit), 0);
    }

    private VMExecutor(Backend backend, CallData callData, StackResourcePool pool, Limit limit, int depth) {
        this.backend = backend;
        this.callData = callData;
        this.limit = limit;
        this.depth = depth;
        this.pool = pool;
    }

    public Backend getBackend() {
        return backend;
    }

    public CallData getCallData() {
        return callData;
    }

    Backend backend;

    CallData callData;

    StackResourcePool pool;


    // gas limit hook
    private Limit limit;
    // call depth
    private int depth;


    public VMExecutor clone() {
        if (depth + 1 == MAX_CALL_DEPTH)
            throw new RuntimeException("vm call depth overflow");
        return new VMExecutor(backend, callData.clone(), pool, limit, depth + 1);
    }

    public VMResult execute() {

        // 1. increase sender nonce
        long n = backend.getNonce(callData.getOrigin());
        if (!backend.isStatic() && n != callData.getTxNonce() && callData.getCallType() != CallType.COINBASE)
            throw new RuntimeException("invalid nonce");

        HexBytes contractAddress = Address.empty();

        if (callData.getCallType() == CallType.CALL) {
            backend.setNonce(callData.getOrigin(), n + 1);
            // contract deploy nonce will increase internally
        }

        if (callData.getCallType() == CallType.CREATE) {
            contractAddress = HashUtil.calcNewAddrHex(
                callData.getCaller().getBytes(),
                callData.getTxNonceAsBytes()
            );
            callData.setTo(contractAddress);
        }

        // 2. set initial gas by payload size
        if(callData.getCallType() != CallType.COINBASE)
            limit.setInitialGas(backend.getInitialGas(callData.getCallType() == CallType.CREATE, callData.getData().getBytes()));
        byte[] result = executeInternal();

        // 3. calculate fee and
        Uint256 fee = Uint256.of(limit.getGas()).times(callData.getGasPrice());
        backend.subBalance(callData.getOrigin(), fee);


        return new VMResult(
            limit.getGas(),
            contractAddress,
            result,
            Collections.emptyList(),
            fee
        );
    }

    @SneakyThrows
    public byte[] executeInternal() {
        switch (callData.getCallType()) {
            case COINBASE: {
                backend.addBalance(callData.getTxTo(), callData.getTxValue());
                for (BuiltinContract bios : backend.getBios().values()) {
                    return bios.call(backend, callData);
                }
                return ByteUtil.EMPTY_BYTE_ARRAY;
            }
            case CALL:
            case CREATE: {
                // is prebuilt
                if (backend.getBuiltins().containsKey(callData.getTo())) {
                    backend.addBalance(callData.getTo(), callData.getValue());
                    backend.subBalance(callData.getCaller(), callData.getValue());
                    BuiltinContract updater = backend.getBuiltins().get(callData.getTo());
                    return updater.call(backend, callData);
                }

                Module module;
                // contract constructor/call arguments
                byte[] data;
                HexBytes receiver = callData.getTo();
                boolean create = callData.getCallType() == CallType.CREATE;

                // if this is contract deploy, should create contract account
                if (create) {
                    // increase sender nonce
                    long n = backend.getNonce(callData.getCaller());
                    Module tmpModule = new Module(callData.getData().getBytes());

                    // validate module
                    ModuleValidator.INSTANCE.validate(tmpModule, false);

                    byte[] code = WBI.dropInit(callData.getData().getBytes());
                    data = WBI.extractInitData(tmpModule);

                    // increase nonce here to avoid conflicts
                    backend.setNonce(callData.getCaller(), n + 1);
                    backend.setCode(receiver, HexBytes.fromBytes(code));
                    module = new Module(code);
                } else {
                    HexBytes hash = backend.getContractHash(receiver);
                    // this is a transfer transaction
                    if(hash.equals(HashUtil.EMPTY_DATA_HASH_HEX))
                        module = null;
                    else
                    module = CACHE.get(backend.getContractHash(receiver), () -> {
                        byte[] code = backend.getCode(receiver).getBytes();
                        if(code == null || code.length == 0)
                            throw new RuntimeException("contract code not found");
                        return new Module(code);
                    });
                    data = callData.getData().getBytes();
                }

                // call a non-contract account
                if (module == null && !callData.getData().isEmpty())
                    throw new RuntimeException("call receiver not a contract");
                // transfer to a contract account
                if (module != null && callData.getData().isEmpty()) {
                    throw new RuntimeException("transfer to a contract");
                }

                backend.addBalance(receiver, callData.getValue());
                backend.subBalance(callData.getCaller(), callData.getValue());

                if (module == null)
                    return ByteUtil.EMPTY_BYTE_ARRAY;

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


                Abi abi = module.getCustomSections()
                    .stream().filter(x -> x.getName().equals(WBI.ABI_SECTION_NAME))
                    .findFirst()
                    .map(x -> Abi.fromJson(new String(x.getData(), StandardCharsets.UTF_8)))
                    .get();


                long[] rets;
                ModuleInstance instance;
                WBI.InjectResult r;

                try (StackResource stack = pool.tryGet(); Memory mem = new UnsafeMemory()) {
                    instance = ModuleInstance
                        .builder()
                        .stackProvider(stack)
                        .module(module)
                        .memory(mem)
                        .hooks(Collections.singleton(limit))
                        .hostFunctions(hosts.getAll())
                        .build();

                    r = WBI.inject(create, abi, instance, HexBytes.fromBytes(data));

                    // payable check
                    boolean payable = r.getEntry() != null && r.getEntry().isPayable();
                    if (!payable && !callData.getValue().isZero())
                        throw new RuntimeException("function " + r.getName() + " is not payable");

                    if (!r.getExecutable())
                        return ByteUtil.EMPTY_BYTE_ARRAY;

                    rets = instance.execute(r.getName(), r.getPointers());
                    List<Object> results = new ArrayList<>();
                    List<Abi.Entry.Param> outputs;

                    if (create) {
                        outputs = Optional
                            .ofNullable(abi.findConstructor())
                            .map(x -> x.outputs)
                            .orElse(Collections.emptyList());
                    } else {
                        outputs = abi.findFunction(x -> x.name.equals(r.getName())).outputs;
                        if (outputs == null)
                            outputs = Collections.emptyList();
                    }


                    // extract result
                    for (int i = 0; i < rets.length; i++) {
                        SolidityType type = outputs.get(i).type;
                        switch (type.getName()) {
                            case "uint8":
                            case "uint16":
                            case "uint32":
                            case "uint64": {
                                results.add(BigInteger.valueOf(rets[i]));
                                break;
                            }
                            case "uint":
                            case "uint256": {
                                Uint256 u = (Uint256) WBI.peek(instance, (int) rets[i], WbiType.UINT_256);
                                results.add(u.getValue());
                                break;
                            }
                            case "string": {
                                String s = (String) WBI.peek(instance, (int) rets[i], WbiType.STRING);
                                results.add(s);
                                break;
                            }
                            case "address": {
                                HexBytes addr = (HexBytes) WBI.peek(instance, (int) rets[i], WbiType.ADDRESS);
                                results.add(addr.getBytes());
                                break;
                            }
                            default: {
                                if (type.getName().endsWith("]") || type.getName().endsWith(")")) {
                                    throw new RuntimeException("array or tuple is not supported");
                                }

                                if (type.getName().startsWith("bytes")) {
                                    HexBytes bytes = (HexBytes) WBI.peek(instance, (int) rets[i], WbiType.BYTES);
                                    results.add(bytes.getBytes());
                                }
                                break;
                            }
                        }
                    }

                    if (outputs.size() > 0) {
                        return Abi.Entry.Param.encodeList(outputs, results.toArray());
                    }

                    return ByteUtil.EMPTY_BYTE_ARRAY;
                }
            }
            default:
                throw new UnsupportedOperationException();
        }
    }
}
