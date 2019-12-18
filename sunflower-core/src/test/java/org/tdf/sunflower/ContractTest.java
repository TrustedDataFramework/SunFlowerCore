package org.tdf.sunflower;

import com.google.common.io.ByteStreams;
import com.google.common.primitives.Bytes;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import org.tdf.common.util.HexBytes;
import org.tdf.rlp.RLPCodec;
import org.tdf.sunflower.types.Transaction;
import org.tdf.sunflower.account.PublicKeyHash;
import org.tdf.sunflower.consensus.poa.PoAConstants;
import org.tdf.sunflower.util.FileUtils;
import org.tdf.crypto.ed25519.Ed25519PrivateKey;

import java.nio.charset.StandardCharsets;

public class ContractTest {
    private static final String TEST_PRIVATE_KEY = "04ba73067f0c0c862d709ee72daac28275847ab900abddacc90bccbbf3bfc04c";
    private static final String TEST_PUBLIC_KEY = "2e9ee2c2daab350d9c789ef348588e289f633dd867db64311ad3a61cf5f671f4";
    private static final String TEST_ADDRESS = "13KDQQwDtkCVaPnhFS3c81yszFw3Sd9r5R";


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
        byte[] sig = new Ed25519PrivateKey(HexBytes.parse(TEST_PRIVATE_KEY).getBytes()).sign(RLPCodec.encode(t));
        t.setSignature(new HexBytes(sig));
        RestTemplate client = new RestTemplate();


        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<byte[]> httpEntity = new HttpEntity<>(Start.MAPPER.writeValueAsBytes(t),headers);

        ResponseEntity<String> resp = client
                .exchange(System.getenv("URL"), HttpMethod.POST, httpEntity, String.class);
        System.out.println(resp.getBody());
    }

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
        byte[] sig = new Ed25519PrivateKey(HexBytes.parse(TEST_PRIVATE_KEY).getBytes()).sign(RLPCodec.encode(t));
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
