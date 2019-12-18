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
import org.tdf.common.util.LittleEndian;
import org.tdf.crypto.ed25519.Ed25519PrivateKey;

import java.nio.charset.StandardCharsets;

public class CoinContractTest {

    private static final String TEST_PRIVATE_KEY = "a9ce809d201b28e3fd00b269ab042ed27c6ccd6d330a03c13102556b9c958178";
    private static final String TEST_PUBLIC_KEY = "6db6eef88329fdfed125fef83b529e5f4d396b44fb1ed8d096700d72a6424720";

    private static final String TEST_RECIPIENT_PUBLIC_KEY = "36ddb2d6686a827e7edc751f7304d59ea749cd045a7945a028cb4d92a71db870";

    public void testDeployContract() throws Exception {
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

        HttpEntity<byte[]> httpEntity = new HttpEntity<>(Start.MAPPER.writeValueAsBytes(t), headers);

        ResponseEntity<String> resp = client
                .exchange(System.getenv("URL"), HttpMethod.POST, httpEntity, String.class);
        System.out.println(resp.getBody());
    }

    public void testContractCall() throws Exception {
        HexBytes from = HexBytes.parse(TEST_PUBLIC_KEY);
        PublicKeyHash to = PublicKeyHash.fromPublicKey(from.getBytes());
        PublicKeyHash recipient = PublicKeyHash.fromPublicKey(HexBytes.parse(TEST_RECIPIENT_PUBLIC_KEY).getBytes());
        byte[] method = "transfer".getBytes(StandardCharsets.US_ASCII);
        Transaction t = Transaction.builder()
                .version(PoAConstants.TRANSACTION_VERSION)
                .createdAt(System.currentTimeMillis() / 1000)
                .from(from)
                .type(Transaction.Type.CONTRACT_CALL.code)
                .to(new HexBytes(to.getPublicKeyHash()))
                .payload(new HexBytes(Bytes.concat(new byte[]{(byte) method.length},
                        method,
                        new byte[]{(byte)recipient.getPublicKeyHash().length},
                        recipient.getPublicKeyHash(),
                        LittleEndian.encodeInt64(1000)
                )))
                .build();

        byte[] sig = new Ed25519PrivateKey(HexBytes.parse(TEST_PRIVATE_KEY).getBytes()).sign(RLPCodec.encode(t));
        t.setSignature(new HexBytes(sig));
        RestTemplate client = new RestTemplate();


        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<byte[]> httpEntity = new HttpEntity<>(Start.MAPPER.writeValueAsBytes(t), headers);

        ResponseEntity<String> resp = client
                .exchange(System.getenv("URL"), HttpMethod.POST, httpEntity, String.class);
        System.out.println(resp.getBody());
    }

}
