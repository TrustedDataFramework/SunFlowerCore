package org.tdf.sunflower.vm.hosts;

import org.tdf.common.types.Uint256;
import org.tdf.common.util.HexBytes;
import org.tdf.lotusvm.runtime.HostFunction;
import org.tdf.lotusvm.types.FunctionType;
import org.tdf.lotusvm.types.ValueType;
import org.tdf.sunflower.state.Account;
import org.tdf.sunflower.vm.abi.AbiDataType;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;


public class Transfer extends HostFunction {
    private final Map<HexBytes, Account> states;
    private final HexBytes contractAddress;
    private final boolean readonly;

    public Transfer(Map<HexBytes, Account> states, HexBytes contractAddress, boolean readonly) {
        this.states = states;
        this.contractAddress = contractAddress;
        this.readonly = readonly;
        setType(
                new FunctionType(
                        Arrays.asList(
                                ValueType.I64, ValueType.I64,
                                ValueType.I64
                        ),
                        Collections.emptyList()
                )
        );
        setName("_transfer");
    }

    @Override
    public long[] execute(long... parameters) {
        if (readonly)
            throw new RuntimeException("transfer is not allowed here");
        if (parameters[0] != 0) {
            throw new RuntimeException("unexpected");
        }
        HexBytes toAddr = HexBytes.fromBytes((byte[]) WasmBlockChainInterface.peek(getInstance(), (int) parameters[1], AbiDataType.ADDRESS));
        Uint256 amount = (Uint256) WasmBlockChainInterface.peek(getInstance(), (int) parameters[2], AbiDataType.U256);
        Account contractAccount = states.get(this.contractAddress);
        contractAccount.subBalance(amount);
        states.putIfAbsent(toAddr, Account.emptyAccount(toAddr));
        Account to = states.get(toAddr);
        to.addBalance(amount);
        states.put(contractAddress, contractAccount);
        states.put(to.getAddress(), to);
        return new long[0];
    }
}
