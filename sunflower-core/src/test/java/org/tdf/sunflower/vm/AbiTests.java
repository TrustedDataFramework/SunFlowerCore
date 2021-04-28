package org.tdf.sunflower.vm;

import org.checkerframework.checker.units.qual.C;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.tdf.common.serialize.Codec;
import org.tdf.common.serialize.Codecs;
import org.tdf.common.store.ByteArrayMapStore;
import org.tdf.common.trie.Trie;
import org.tdf.common.util.HexBytes;
import org.tdf.crypto.CryptoHelpers;
import org.tdf.lotusvm.ModuleInstance;
import org.tdf.lotusvm.runtime.HostFunction;
import org.tdf.lotusvm.types.Module;
import org.tdf.sunflower.pool.BackendImpl;
import org.tdf.sunflower.state.Account;
import org.tdf.sunflower.state.Address;
import org.tdf.sunflower.types.CryptoContext;
import org.tdf.sunflower.types.Header;
import org.tdf.sunflower.types.Transaction;
import org.tdf.sunflower.vm.hosts.Limit;
import org.tdf.sunflower.vm.hosts.Log;
import org.tdf.sunflower.vm.hosts.U256Host;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

@RunWith(JUnit4.class)
public class AbiTests {

    @Test
    public void test0() throws Exception{
        Codec<HexBytes> k = Codec.newInstance(HexBytes::getBytes, HexBytes::fromBytes);
        Codec<Account> v = Codecs.newRLPCodec(Account.class);

        Backend backend = new BackendImpl(
                null,
                null,
                Trie.<HexBytes, Account>builder().hashFunction(CryptoHelpers::keccak256).keyCodec(k).valueCodec(v).store(new ByteArrayMapStore<>()).build(),
                Trie.<byte[], byte[]>builder().hashFunction(CryptoHelpers::keccak256).keyCodec(Codec.identity()).store(new ByteArrayMapStore<>()).valueCodec(Codec.identity()).build(),
                new HashMap<>(),
                new HashMap<>(),
                new HashMap<>(),
                new HashMap<>(),
                false,
                new ByteArrayMapStore<>(),
                new HashMap<>(),
                new HashMap<>(),
                0
        );

        CryptoContext.keccak256 = CryptoHelpers::keccak256;

        byte[] code = Files.readAllBytes(Paths.get("/Users/sal/Documents/Github/SunFlowerCore/sdk-v2/bin"));

        HexBytes contractAddress = Transaction.createContractAddress(Address.empty(), 1);
        CallData callData = CallData.empty();
        callData.setTxNonce(1);
        callData.setCallType(Transaction.Type.CONTRACT_DEPLOY.code);
        callData.setData(HexBytes.fromBytes(code));
        VMExecutor executor = new VMExecutor(backend, callData, new Limit(), 0);
        executor.execute();

        callData = CallData.empty();
        callData.setTxNonce(2);
        callData.setCallType(Transaction.Type.CONTRACT_CALL.code);
        callData.setTo(contractAddress);
        callData.setData(HexBytes.fromHex("64561ec300000000000000000000000000000000000000000000000000000000000000a000000000000000000000000012691d3b267130bb57e622f880c6928d4ed9fac70000000000000000000000000000000000000000000000008ac7230489e80000000000000000000000000000000000000000000000000000000000000003944700000000000000000000000000000000000000000000000000000000000000e00000000000000000000000000000000000000000000000000000000000000004736f6d6500000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000001412691d3b267130bb57e622f880c6928d4ed9fac7000000000000000000000000"));
        executor = new VMExecutor(backend, callData, new Limit(), 0);
        executor.execute();
    }
}
