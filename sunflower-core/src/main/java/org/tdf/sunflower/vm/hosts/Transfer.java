package org.tdf.sunflower.vm.hosts;

import org.tdf.common.util.HexBytes;
import org.tdf.lotusvm.runtime.HostFunction;
import org.tdf.lotusvm.types.FunctionType;
import org.tdf.lotusvm.types.ValueType;
import org.tdf.sunflower.state.Account;
import org.tdf.sunflower.state.SafeMath;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;


public class Transfer extends HostFunction {
    private final Map<HexBytes, Account> states;
    private final HexBytes contractAddress;

    public Transfer(Map<HexBytes, Account> states, HexBytes contractAddress) {
        this.states = states;
        this.contractAddress = contractAddress;
        setType(
                new FunctionType(
                        Arrays.asList(
                                ValueType.I32, ValueType.I64,
                                ValueType.I64, ValueType.I64,
                                ValueType.I64, ValueType.I64
                        ),
                        Collections.emptyList()
                )
        );
        setName("_transfer");
    }

    @Override
    public long[] execute(long... parameters) {
        if (parameters[0] != 0) {
            throw new RuntimeException("unexpected");
        }
        long amount = parameters[1];
        Account contractAccount = states.get(this.contractAddress);
        HexBytes toAddr = HexBytes.fromBytes(loadMemory((int) parameters[2], (int) parameters[3]));
        contractAccount.setBalance(SafeMath.sub(contractAccount.getBalance(), amount));
        states.putIfAbsent(toAddr, Account.emptyAccount(toAddr));
        Account to = states.get(toAddr);
        to.setBalance(SafeMath.add(to.getBalance(), amount));
        states.put(contractAddress, contractAccount);
        states.put(to.getAddress(), to);
        return new long[0];
    }
}
