package org.tdf.sunflower.vm;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.tdf.common.types.Uint256;
import org.tdf.common.util.ByteUtil;
import org.tdf.common.util.HashUtil;
import org.tdf.common.util.HexBytes;
import org.tdf.evm.EvmCallData;
import org.tdf.evm.EvmContext;
import org.tdf.evm.Interpreter;
import org.tdf.lotusvm.Module;
import org.tdf.lotusvm.ModuleInstance;
import org.tdf.lotusvm.runtime.Memory;
import org.tdf.lotusvm.runtime.ResourceFactory;
import org.tdf.lotusvm.runtime.StackAllocator;
import org.tdf.sunflower.facade.RepositoryReader;
import org.tdf.sunflower.state.Address;
import org.tdf.sunflower.state.BuiltinContract;
import org.tdf.sunflower.types.VMResult;
import org.tdf.sunflower.vm.abi.Abi;
import org.tdf.sunflower.vm.abi.SolidityType;
import org.tdf.sunflower.vm.abi.WbiType;
import org.tdf.sunflower.vm.hosts.*;

import java.io.OutputStream;
import java.io.PrintStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@NoArgsConstructor
@Slf4j(topic = "vm")
public class VMExecutor {
    public static final int MAX_FRAMES = 16384;
    public static final int MAX_STACK_SIZE = MAX_FRAMES * 4;
    public static final int MAX_LABELS = MAX_FRAMES * 4;
    public static final int MAX_CALL_DEPTH = 8;
    public static final int EVM_MAX_STACK_SIZE = 1024;
    // 16 mb
    public static final int EVM_MAX_MEMORY_SIZE = 1024 * 1024 * 16;

    private static String outDirectory = "";

    private static final AtomicInteger COUNTER = new AtomicInteger();

    public static void enableDebug(String outDirectory) {
        VMExecutor.outDirectory = outDirectory;
    }

    @SneakyThrows
    private static PrintStream getPrintStream() {
        if (outDirectory.isEmpty())
            return null;

        String filename = String.format("%04d.log", COUNTER.incrementAndGet());
        Path path = Paths.get(outDirectory, filename);

        log.info("write vm debug log to file {}", path);
        OutputStream os = Files.newOutputStream(
            path,
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING,
            StandardOpenOption.WRITE
        );

        return new PrintStream(os);
    }


    public static final Cache<HexBytes, byte[]> CACHE =
        CacheBuilder
            .newBuilder()
            .weigher((k, v) -> ((byte[]) v).length + ((HexBytes) k).size())
            .maximumWeight(1024L * 1024L * 8L) // 8mb cache for contracts
            .build();


    public VMExecutor(RepositoryReader rd, Backend backend, CallData callData, long gasLimit) {
        this(rd, backend, callData, new Limit(gasLimit), 0);
    }

    private RepositoryReader rd;

    private VMExecutor(RepositoryReader rd, Backend backend, CallData callData, Limit limit, int depth) {
        this.rd = rd;
        this.backend = backend;
        this.callData = callData;
        this.limit = limit;
        this.depth = depth;
    }

    public Backend getBackend() {
        return backend;
    }

    public CallData getCallData() {
        return callData;
    }

    Backend backend;

    CallData callData;

    // gas limit hook
    private Limit limit;
    // call depth
    private int depth;


    public VMExecutor clone() {
        if (depth + 1 == MAX_CALL_DEPTH)
            throw new RuntimeException("vm call depth overflow");
        return new VMExecutor(rd, backend, callData.clone(), limit, depth + 1);
    }

    public VMResult execute() {
        // 1. increase sender nonce
        long n = backend.getNonce(callData.getOrigin());
        if (n != callData.getTxNonce() && callData.getCallType() != CallType.COINBASE)
            throw new RuntimeException("invalid nonce");

        HexBytes contractAddress = Address.empty();

        if (!backend.getStaticCall() && callData.getCallType() == CallType.CALL) {
            backend.setNonce(callData.getOrigin(), n + 1);
            // contract deploy nonce will increase in executeInternal
        }

        if (callData.getCallType() == CallType.CREATE) {
            contractAddress = HashUtil.calcNewAddrHex(
                callData.getCaller().getBytes(),
                callData.getTxNonceAsBytes()
            );
            callData.setTo(contractAddress);
        }

        // 2. set initial gas by payload size
        if (callData.getCallType() != CallType.COINBASE)
            limit.setInitialGas(backend.getInitialGas(callData.getCallType() == CallType.CREATE, callData.getData().getBytes()));
        byte[] result = executeInternal();

        // 3. calculate fee and
        Uint256 fee = Uint256.of(limit.getGas()).times(callData.getGasPrice());
        backend.subBalance(callData.getOrigin(), fee);


        return new VMResult(
            limit.getGas(),
            contractAddress,
            HexBytes.fromBytes(result),
            Collections.emptyList(),
            fee
        );
    }

    private static final byte[] WASM_MAGIC = {0x00, 0x61, 0x73, 0x6d};

    private boolean isWasm(byte[] bytes) {
        return bytes.length >= WASM_MAGIC.length && Arrays.equals(bytes, 0, WASM_MAGIC.length, WASM_MAGIC, 0, WASM_MAGIC.length);
    }


    @SneakyThrows
    public byte[] executeInternal() {
        switch (callData.getCallType()) {
            case COINBASE: {
                backend.addBalance(callData.getTxTo(), callData.getTxValue());
                for (BuiltinContract bios : backend.getBios().values()) {
                    return bios.call(rd, backend, callData);
                }
                return ByteUtil.EMPTY_BYTE_ARRAY;
            }
            case DELEGATE:
            case CALL:
            case CREATE: {
                // is prebuilt
                if (backend.getBuiltins().containsKey(callData.getTo())) {
                    backend.addBalance(callData.getTo(), callData.getValue());
                    backend.subBalance(callData.getCaller(), callData.getValue());
                    BuiltinContract updater = backend.getBuiltins().get(callData.getTo());
                    return updater.call(rd, backend, callData);
                }

                byte[] code;
                // contract constructor/call arguments
                byte[] data;
                // if call context is evm, else web assembly
                boolean isWasm;

                HexBytes receiver = callData.getTo();
                boolean create = callData.getCallType() == CallType.CREATE;


                switch (callData.getCallType()) {
                    case CREATE: {
                        isWasm = isWasm(callData.getData().getBytes());

                        // increase sender nonce
                        long n = backend.getNonce(callData.getCaller());

                        if (isWasm) {
                            try (Module tmpModule = Module.create(callData.getData().getBytes())) {
                                // validate module
                                ModuleValidator.INSTANCE.validate(tmpModule, false);

                                code = WBI.dropInit(callData.getData().getBytes());
                                data = WBI.extractInitData(tmpModule);
                                backend.setCode(receiver, HexBytes.fromBytes(code));
                            }
                        } else {
                            code = callData.getData().getBytes();
                            data = ByteUtil.EMPTY_BYTE_ARRAY;
                        }

                        // increase nonce here to avoid conflicts
                        backend.setNonce(callData.getCaller(), n + 1);
                        break;
                    }
                    case CALL: {
                        HexBytes hash = backend.getContractHash(receiver);
                        // this is a transfer transaction
                        if (hash.equals(HashUtil.EMPTY_DATA_HASH_HEX)) {
                            code = HexBytes.EMPTY_BYTES;
                        } else {
                            code = CACHE.get(backend.getContractHash(receiver), () -> backend.getCode(receiver).getBytes());
                        }
                        data = callData.getData().getBytes();
                        isWasm = isWasm(code);
                        break;
                    }
                    case DELEGATE: {
                        HexBytes hash = backend.getContractHash(callData.getDelegateAddr());
                        // this is a transfer transaction
                        if (hash.equals(HashUtil.EMPTY_DATA_HASH_HEX)) {
                            code = HexBytes.EMPTY_BYTES;
                        } else {
                            code = CACHE.get(backend.getContractHash(receiver), () -> backend.getCode(receiver).getBytes());
                        }
                        data = callData.getData().getBytes();
                        isWasm = isWasm(code);
                        break;
                    }
                    default:
                        throw new UnsupportedOperationException();
                }

                Objects.requireNonNull(code);
                // call a non-contract account
                if (code.length == 0 && !callData.getData().isEmpty())
                    throw new RuntimeException("call receiver not a contract");
                // transfer to a contract account
                if (code.length != 0 && callData.getData().isEmpty()) {
                    throw new RuntimeException("transfer to a contract");
                }

                backend.addBalance(receiver, callData.getValue());
                backend.subBalance(callData.getCaller(), callData.getValue());

                if (code.length == 0)
                    return ByteUtil.EMPTY_BYTE_ARRAY;

                if (isWasm)
                    return executeWasm(create, code, data);
                else
                    return executeEvm(create, code, data);
            }
            default:
                throw new UnsupportedOperationException();
        }
    }

    private byte[] executeEvm(boolean create, byte[] code, byte[] input) {
        EvmCallData evmCallData = new EvmCallData(
            callData.getCaller().getBytes(),
            callData.getTo().getBytes(),
            callData.getValue().getValue(),
            input,
            code
        );

        EvmContext ctx = new EvmContext();
        EvmHostImpl host = new EvmHostImpl(this, rd);

        Interpreter interpreter = new Interpreter(host, ctx, evmCallData, getPrintStream(), EVM_MAX_STACK_SIZE, EVM_MAX_MEMORY_SIZE);
        interpreter.execute();

        if (create) {
            backend.setCode(callData.getTo(), HexBytes.fromBytes(interpreter.getRet()));
        }
        return create ? ByteUtil.EMPTY_BYTE_ARRAY : interpreter.getRet();
    }

    private byte[] executeWasm(boolean create, byte[] code, byte[] data) {
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


        try (
            StackAllocator stack =
                ResourceFactory.createStack(MAX_STACK_SIZE, MAX_FRAMES, MAX_LABELS);
            Memory mem = ResourceFactory.createMemory();
            Module module = Module.create(code);
        ) {
            ModuleInstance instance =
                ModuleInstance
                    .builder()
                    .stackAllocator(stack)
                    .module(module)
                    .memory(mem)
                    .hooks(Collections.singleton(limit))
                    .hostFunctions(hosts.getAll())
                    .build();

            Abi abi = module.getCustomSections()
                .stream().filter(x -> x.getName().equals(WBI.ABI_SECTION_NAME))
                .findFirst()
                .map(x -> Abi.fromJson(new String(x.getData(), StandardCharsets.UTF_8)))
                .get();


            long[] rets;
            WBI.InjectResult r;

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
}
