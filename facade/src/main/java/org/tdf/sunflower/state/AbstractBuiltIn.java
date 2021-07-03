package org.tdf.sunflower.state;

import lombok.Getter;
import org.tdf.common.util.FastByteComparisons;
import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.facade.RepositoryReader;
import org.tdf.sunflower.facade.RepositoryService;
import org.tdf.sunflower.types.Header;
import org.tdf.sunflower.vm.Backend;
import org.tdf.sunflower.vm.CallContext;
import org.tdf.sunflower.vm.CallData;
import org.tdf.sunflower.vm.abi.Abi;

import java.util.List;
import java.util.Objects;

public abstract class AbstractBuiltIn implements BuiltinContract {
    protected StateTrie<HexBytes, Account> accounts;
    protected RepositoryService repo;

    @Getter
    protected HexBytes address;

    protected AbstractBuiltIn(
        HexBytes address,
        StateTrie<HexBytes, Account> accounts,
        RepositoryService repo
    ) {
        this.address = address;
        this.accounts = accounts;
        this.repo = repo;
    }

    protected byte[] getSelector(HexBytes data) {
        return data.slice(0, 4).getBytes();
    }

    @Override
    public byte[] call(RepositoryReader rd, Backend backend, CallContext ctx, CallData callData) {
        Abi.Function func = getFunction(callData.getData());
        List<?> inputs = func.decode(callData.getData().getBytes());
        List<?> results = call(rd, backend, ctx, callData, func.name, inputs.toArray());
        return Abi.Entry.Param.encodeList(func.outputs, results.toArray());
    }

    protected Abi.Function getFunction(HexBytes data) {
        byte[] selector = getSelector(data);
        Abi abi = getAbi();
        Abi.Function func = Objects.requireNonNull(
            abi.findFunction(x -> FastByteComparisons.equal(x.encodeSignature(), selector))
        );
        return func;
    }

    protected Abi.Function getFunction(String method) {
        Abi abi = getAbi();
        Abi.Function func = Objects.requireNonNull(
            abi.findFunction(x -> x.name.equals(method))
        );
        return func;
    }

    @Override
    public List<?> view(RepositoryReader rd, HexBytes blockHash, String method, Object... args) {
        Header parent = rd.getHeaderByHash(blockHash);
        Abi.Function func = getFunction(method);
        byte[] encoded = func.encode(args);
        CallData callData = Utils.INSTANCE.createCallData(encoded);
        return call(rd, accounts.createBackend(parent, null, true, parent.getStateRoot()), CallContext.empty(), callData, method, args);
    }
}
