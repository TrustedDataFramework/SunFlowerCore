package org.tdf.sunflower.vm.hosts;

import org.tdf.common.util.HexBytes;
import org.tdf.lotusvm.runtime.HostFunction;
import org.tdf.lotusvm.types.FunctionType;
import org.tdf.lotusvm.types.ValueType;
import org.tdf.sunflower.state.Account;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;


public class Transfer extends HostFunction {
    private final HexBytes callerAddress;
    private final Map<HexBytes, Account> states;
    private final HexBytes createdBy;

    public Transfer(HexBytes callerAddress, Map<HexBytes, Account> states, HexBytes createdBy) {
        this.callerAddress = callerAddress;
        this.states = states;
        this.createdBy = createdBy;
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
        if (parameters[0] == 0) {
            long amount = parameters[1];
            Account createdBy = states.get(this.createdBy);
            if (createdBy.getBalance() < amount)
                throw new RuntimeException(this.callerAddress + " balance = " + createdBy.getBalance() + " while transfer amount = " + amount);
            createdBy.setBalance(createdBy.getBalance() - amount);
            Account caller = states.get(callerAddress);
            caller.setBalance(caller.getBalance() + amount);
            return new long[0];
        }
        throw new RuntimeException("unexpected");
    }
}
