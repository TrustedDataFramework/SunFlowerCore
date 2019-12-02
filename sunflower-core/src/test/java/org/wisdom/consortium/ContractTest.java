package org.wisdom.consortium;

import com.google.common.io.ByteStreams;
import com.google.common.primitives.Bytes;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import org.wisdom.common.HexBytes;
import org.wisdom.common.Transaction;
import org.wisdom.consortium.account.PublicKeyHash;
import org.wisdom.consortium.consensus.poa.PoAConstants;
import org.wisdom.consortium.util.FileUtils;
import org.wisdom.crypto.ed25519.Ed25519PrivateKey;
import org.wisdom.util.CommonUtil;

import java.nio.charset.StandardCharsets;

@RunWith(JUnit4.class)
public class ContractTest {
    private static final String TEST_PRIVATE_KEY = "04ba73067f0c0c862d709ee72daac28275847ab900abddacc90bccbbf3bfc04c";
    private static final String TEST_PUBLIC_KEY = "2e9ee2c2daab350d9c789ef348588e289f633dd867db64311ad3a61cf5f671f4";
    private static final String TEST_ADDRESS = "13KDQQwDtkCVaPnhFS3c81yszFw3Sd9r5R";


    @Test
    public void testDeployContract() throws Exception{
        byte[] binary = ByteStreams.toByteArray(FileUtils.getResource(System.getenv("FILE_PATH")).getInputStream());
        HexBytes from = HexBytes.parse(TEST_PUBLIC_KEY);
        PublicKeyHash to = PublicKeyHash.fromPublicKey(from.getBytes());
        Transaction t = Transaction.builder()
                .version(PoAConstants.TRANSACTION_VERSION)
                .createdAt(System.currentTimeMillis() / 1000)
                .from(from)
                .type(Transaction.Type.CONTRACT_DEPLOY.code)
                .to(new HexBytes(to.getPublicKeyHash()))
                .payload(new HexBytes(binary))
                .build();
        byte[] sig = new Ed25519PrivateKey(HexBytes.parse(TEST_PRIVATE_KEY).getBytes()).sign(CommonUtil.getRaw(t));
        t.setSignature(new HexBytes(sig));
        RestTemplate client = new RestTemplate();


        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<byte[]> httpEntity = new HttpEntity<>(Start.MAPPER.writeValueAsBytes(t),headers);

        ResponseEntity<String> resp = client
                .exchange(System.getenv("URL"), HttpMethod.POST, httpEntity, String.class);
        System.out.println(resp.getBody());
    }

    @Test
    public void testContractCall() throws Exception{
        HexBytes from = HexBytes.parse(TEST_PUBLIC_KEY);
        PublicKeyHash to = PublicKeyHash.fromPublicKey(from.getBytes());
        byte[] method = "incrementAndGet".getBytes(StandardCharsets.US_ASCII);
        Transaction t = Transaction.builder()
                .version(PoAConstants.TRANSACTION_VERSION)
                .createdAt(System.currentTimeMillis() / 1000)
                .from(from)
                .type(Transaction.Type.CONTRACT_CALL.code)
                .to(new HexBytes(to.getPublicKeyHash()))
                .payload(new HexBytes(Bytes.concat(new byte[]{(byte)method.length}, method)))
                .build();
        byte[] sig = new Ed25519PrivateKey(HexBytes.parse(TEST_PRIVATE_KEY).getBytes()).sign(CommonUtil.getRaw(t));
        t.setSignature(new HexBytes(sig));
        RestTemplate client = new RestTemplate();


        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<byte[]> httpEntity = new HttpEntity<>(Start.MAPPER.writeValueAsBytes(t),headers);

        ResponseEntity<String> resp = client
                .exchange(System.getenv("URL"), HttpMethod.POST, httpEntity, String.class);
        System.out.println(resp.getBody());
    }
}
