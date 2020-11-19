package org.tdf.sunflower.vm.abi;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.tdf.common.store.Store;
import org.tdf.common.trie.Trie;
import org.tdf.common.types.Parameters;
import org.tdf.common.types.Uint256;
import org.tdf.common.util.HexBytes;
import org.tdf.common.util.SafeMath;
import org.tdf.lotusvm.ModuleInstance;
import org.tdf.lotusvm.types.Module;
import org.tdf.rlp.RLPCodec;
import org.tdf.rlp.RLPElement;
import org.tdf.rlp.RLPItem;
import org.tdf.rlp.RLPList;
import org.tdf.sunflower.ApplicationConstants;
import org.tdf.sunflower.state.Account;
import org.tdf.sunflower.types.*;
import org.tdf.sunflower.vm.hosts.*;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;

@RequiredArgsConstructor
@Getter
public class ContractCall {
    public static final Set<String> RESERVED_FUNCTIONS = new HashSet<>(Arrays.asList("__alloc", "__retain", "__setArgumentsLength", "__collect", "__release", "__idof"));
    // contract address already called
    private final Map<HexBytes, Account> states;
    private final Header header;
    private final Transaction transaction;

    private final Function<byte[], Trie<byte[], byte[]>> storageTrieSupplier;

    // contract code store
    private final Store<byte[], byte[]> contractStore;

    // gas limit hook
    private final Limit limit;

    // call depth
    private final int depth;

    // msg.sender
    private final HexBytes sender;

    private final boolean readonly;

    // contract address called currently
    private HexBytes recipient;

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
        return WasmBlockChainInterface.malloc(moduleInstance, s);
    }

    public static int allocBytes(ModuleInstance moduleInstance, byte[] buf) {
       return WasmBlockChainInterface.mallocBytes(moduleInstance, buf);
    }

    public static int allocAddress(ModuleInstance moduleInstance, byte[] addr) {
        return WasmBlockChainInterface.mallocAddress(moduleInstance, addr);
    }

    public static int allocU256(ModuleInstance moduleInstance, Uint256 u) {
        return WasmBlockChainInterface.malloc(moduleInstance, u);
    }

    static Object getResult(ModuleInstance module, long offset, AbiDataType type) {
        switch (type) {
            case I64:
            case U64:
            case F64:
            case BOOL: {
                return offset;
            }
            default:
                return WasmBlockChainInterface.mpeek(module, (int) offset, type);
        }
    }

    public ContractCall fork() {
        if (depth + 1 == ApplicationConstants.MAX_CONTRACT_CALL_DEPTH)
            throw new RuntimeException("exceed call max depth");
        return new ContractCall(
                states,
                header,
                transaction,
                storageTrieSupplier,
                contractStore,
                this.limit.fork(),
                this.depth + 1,
                this.recipient,
                this.readonly
        );
    }

    private long[] putParameters(ModuleInstance module, Parameters params) {
        long[] ret = new long[params.getTypes().length];
        for (int i = 0; i < ret.length; i++) {
            AbiDataType t = AbiDataType.values()[(int) params.getTypes()[i]];
            switch (t) {
                case I64:
                case U64:
                case F64:
                case BOOL: {
                    ret[i] = params.getLi().get(i).asLong();
                    break;
                }
                case BYTES: {
                    ret[i] = allocBytes(module, params.getLi().get(i).asBytes());
                    break;
                }
                case STRING: {
                    ret[i] = allocString(module, params.getLi().get(i).asString());
                    break;
                }
                case U256: {
                    ret[i] = allocU256(module, Uint256.of(params.getLi().get(i).asBytes()));
                    break;
                }
                case ADDRESS: {
                    ret[i] = allocAddress(module, params.getLi().get(i).asBytes());
                    break;
                }
                default:
                    throw new UnsupportedOperationException();
            }
        }
        return ret;
    }

    public TransactionResult call(HexBytes binaryOrAddress, String method, Parameters parameters, Uint256 amount, boolean returnAddress, List<ContractABI> contractABIs) {
        if (RESERVED_FUNCTIONS.contains(method))
            throw new RuntimeException(method + " is reserved");
        boolean isDeploy = "init".equals(method);
        Account contractAccount;
        Account originAccount = readonly ? null : states.get(this.transaction.getFromAddress());
        Module m = null;
        HexBytes contractAddress;

        if (isDeploy) {
            if (this.readonly)
                throw new RuntimeException("cannot deploy contract here");
            m = new Module(binaryOrAddress.getBytes());
            byte[] hash = CryptoContext.hash(binaryOrAddress.getBytes());
            originAccount.setNonce(SafeMath.add(originAccount.getNonce(), 1));
            contractStore.put(hash, binaryOrAddress.getBytes());
            contractAddress = Transaction.createContractAddress(transaction.getFromAddress(), originAccount.getNonce());

            contractAccount = Account.emptyContract(contractAddress);
            contractAccount.setContractHash(hash);
            contractAccount.setCreatedBy(this.transaction.getFromAddress());
            contractAccount.setNonce(originAccount.getNonce());
        } else {
            contractAddress = binaryOrAddress;
            contractAccount = states.get(contractAddress);
        }

        // transfer amount from origin account to contract account
        if (!readonly) {
            originAccount.subBalance(amount);
            contractAccount.addBalance(amount);
            states.put(contractAccount.getAddress(), contractAccount);
            states.put(originAccount.getAddress(), originAccount);
        }


        this.recipient = contractAddress;
        assertContractAddress(contractAddress);


        // build Parameters here
        Context ctx = new Context(
                header,
                transaction,
                contractAccount,
                sender,
                amount
        );

        DBFunctions DBFunctions = new DBFunctions(
                storageTrieSupplier.apply(contractAccount.getStorageRoot()),
                this.readonly
        );

        if (isDeploy && contractABIs != null) {
            DBFunctions.getStorageTrie().put("__abi".getBytes(StandardCharsets.UTF_8), RLPCodec.encode(contractABIs));
            contractAccount.setStorageRoot(DBFunctions.getStorageTrie().commit());
            states.put(contractAddress, contractAccount);
        }

        Hosts hosts = new Hosts()
                .withTransfer(
                        states,
                        this.recipient,
                        readonly
                )
                .withReflect(new Reflect(this, readonly))
                .withContext(new ContextHost(ctx, states, contractStore, storageTrieSupplier, readonly))
                .withDB(DBFunctions)
                .withEvent(contractAccount.getAddress(), readonly);

        // every contract should have a init method
        ModuleInstance instance = ModuleInstance
                .builder()
                .hooks(Collections.singleton(limit))
                .hostFunctions(hosts.getAll())
                .binary(contractStore.get(contractAccount.getContractHash())
                        .orElseThrow(() -> new RuntimeException(
                                "contract " + this.recipient + " not found in db")))
                .build();


        RLPList ret = RLPList.createEmpty();
        if (!isDeploy || instance.containsExport("init")) {
            long steps = limit.getSteps();
            long[] offsets = putParameters(instance, parameters);
            limit.setSteps(steps);

            long[] rets = instance.execute(method, offsets);
            if (parameters.getReturnType().length > 0) {
                ret.add(
                        RLPElement.readRLPTree(getResult(instance, rets[0], AbiDataType.values()[parameters.getReturnType()[0]]))
                );
            }
        }

        if (!readonly) {
            DBFunctions.getStorageTrie().commit();
            contractAccount = states.get(this.recipient);
            contractAccount.setStorageRoot(DBFunctions.getStorageTrie().getRootHash());
            states.put(contractAccount.getAddress(), contractAccount);
        }

        List<Event> events = hosts.getEventHost().getEvents();
        RLPList returns = returnAddress ? RLPList.fromElements(Collections.singleton(RLPItem.fromBytes(contractAddress.getBytes()))) : ret;
        return new TransactionResult(limit.getGas(), returns, events, Uint256.ZERO);
    }
}
