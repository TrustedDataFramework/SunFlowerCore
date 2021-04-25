package org.tdf.sunflower.vm.hosts;

import org.tdf.common.types.Uint256;
import org.tdf.common.util.HexBytes;
import org.tdf.lotusvm.runtime.HostFunction;
import org.tdf.lotusvm.types.FunctionType;
import org.tdf.lotusvm.types.ValueType;
import org.tdf.sunflower.vm.Backend;
import org.tdf.sunflower.vm.abi.AbiDataType;

import java.util.Arrays;
import java.util.Collections;

public class Transfer extends HostFunction {
    private final Backend backend;
    private final HexBytes contractAddress;
    public static final FunctionType FUNCTION_TYPE = new FunctionType(
            Arrays.asList(
                    ValueType.I64, ValueType.I64,
                    ValueType.I64
            ),
            Collections.emptyList()
    );

    public Transfer(Backend backend, HexBytes contractAddress) {
        super("_transfer", FUNCTION_TYPE);
        this.contractAddress = contractAddress;
        this.backend = backend;
    }

    @Override
    public long execute(long... parameters) {
        if (parameters[0] != 0) {
            throw new RuntimeException("unexpected");
        }
        HexBytes toAddr = HexBytes.fromBytes((byte[]) WBI.peek(getInstance(), (int) parameters[1], AbiDataType.ADDRESS));
        Uint256 amount = (Uint256) WBI.peek(getInstance(), (int) parameters[2], AbiDataType.U256);
        Uint256 contractBalance = backend.getBalance(contractAddress);
        backend.setBalance(contractAddress, contractBalance.safeSub(amount));
        Uint256 toBalance = backend.getBalance(toAddr);
        backend.setBalance(toAddr, toBalance.safeAdd(amount));
        return 0;
    }
}
