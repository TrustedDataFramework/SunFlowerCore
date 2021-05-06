package org.tdf.sunflower.vm;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.tdf.sunflower.state.Address;
import org.tdf.sunflower.state.Authentication;
import org.tdf.sunflower.vm.abi.Abi;

import java.util.Collections;

@RunWith(JUnit4.class)
public class AbiTests {

//    @Test
//    public void test0() throws Exception {
//        Codec<HexBytes> k = Codec.newInstance(HexBytes::getBytes, HexBytes::fromBytes);
//        Codec<Account> v = Codecs.newRLPCodec(Account.class);
//
//        Backend backend = new BackendImpl(
//                null,
//                null,
//                Trie.<HexBytes, Account>builder().hashFunction(CryptoHelpers::keccak256).keyCodec(k).valueCodec(v).store(new ByteArrayMapStore<>()).build(),
//                Trie.<HexBytes, HexBytes>builder().hashFunction(CryptoHelpers::keccak256).keyCodec(Codecs.HEX).store(new ByteArrayMapStore<>()).valueCodec(Codecs.HEX).build(),
//                new HashMap<>(),
//                new HashMap<>(),
//                new HashMap<>(),
//                new HashMap<>(),
//                false,
//                new MapStore<>(),
//                new HashMap<>(),
//                new HashMap<>(),
//                0
//        );
//
//        CryptoContext.keccak256 = CryptoHelpers::keccak256;
//
//        byte[] code = Files.readAllBytes(Paths.get("/Users/sal/Documents/Github/SunFlowerCore/sdk-v2/bin"));
//
//        HexBytes contractAddress = Transaction.createContractAddress(Address.empty(), 1);
//        CallData callData = CallData.empty();
//        callData.setTxNonce(1);
//        callData.setCallType(Transaction.Type.CONTRACT_DEPLOY.code);
//        callData.setData(HexBytes.fromBytes(code));
//        VMExecutor executor = new VMExecutor(backend, callData, new Limit(), 0);
//        executor.execute();
//
//        callData = CallData.empty();
//        callData.setTxNonce(2);
//        callData.setCallType(Transaction.Type.CONTRACT_CALL.code);
//        callData.setTo(contractAddress);
//        callData.setData(HexBytes.fromHex("64561ec300000000000000000000000000000000000000000000000000000000000000a000000000000000000000000012691d3b267130bb57e622f880c6928d4ed9fac70000000000000000000000000000000000000000000000008ac7230489e80000000000000000000000000000000000000000000000000000000000000003944700000000000000000000000000000000000000000000000000000000000000e00000000000000000000000000000000000000000000000000000000000000004736f6d6500000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000001412691d3b267130bb57e622f880c6928d4ed9fac7000000000000000000000000"));
//        executor = new VMExecutor(backend, callData, new Limit(), 0);
//        executor.execute();
//
//        callData = CallData.empty();
//        callData.setTxNonce(3);
//        callData.setCallType(Transaction.Type.CONTRACT_CALL.code);
//        callData.setTo(contractAddress);
//        callData.setData(HexBytes.fromHex("f2a75fe4"));
//        executor = new VMExecutor(backend, callData, new Limit(), 0);
//        executor.execute();
//    }

    @Test
    public void test1() {
        Abi.Function func = Authentication.ABI.findFunction(x -> x.name.equals("approved"));

        Abi.Entry.Param.encodeList(func.outputs, Collections.singletonList(Address.empty().getBytes()));
    }
}
