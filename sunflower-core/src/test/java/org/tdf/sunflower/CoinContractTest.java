package org.tdf.sunflower;

import com.google.common.io.ByteStreams;
import com.google.common.primitives.Bytes;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import org.tdf.common.util.HexBytes;
import org.tdf.common.util.LittleEndian;
import org.tdf.crypto.ed25519.Ed25519PrivateKey;
import org.tdf.rlp.RLPCodec;
import org.tdf.sunflower.state.Address;
import org.tdf.sunflower.consensus.poa.PoAConstants;
import org.tdf.sunflower.types.Transaction;
import org.tdf.sunflower.util.FileUtils;

import java.nio.charset.StandardCharsets;

public class CoinContractTest {

    private static final String TEST_PRIVATE_KEY = "a9ce809d201b28e3fd00b269ab042ed27c6ccd6d330a03c13102556b9c958178";
    private static final String TEST_PUBLIC_KEY = "6db6eef88329fdfed125fef83b529e5f4d396b44fb1ed8d096700d72a6424720";

    private static final String TEST_RECIPIENT_PUBLIC_KEY = "36ddb2d6686a827e7edc751f7304d59ea749cd045a7945a028cb4d92a71db870";

    public void testDeployContract() throws Exception {
        byte[] binary = ByteStreams.toByteArray(FileUtils.getInputStream(System.getenv("FILE_PATH")));
        HexBytes from = HexBytes.fromHex(TEST_PUBLIC_KEY);
        HexBytes to = Address.fromPublicKey(from.getBytes());
        Transaction t = Transaction.builder()
                .version(PoAConstants.TRANSACTION_VERSION)
                .createdAt(System.currentTimeMillis() / 1000)
                .from(from)
                .type(Transaction.Type.CONTRACT_DEPLOY.code)
                .to(to)
                .payload(HexBytes.fromBytes(binary))
                .build();
        byte[] sig = new Ed25519PrivateKey(HexBytes.fromHex(TEST_PRIVATE_KEY).getBytes()).sign(RLPCodec.encode(t));
        t.setSignature(HexBytes.fromBytes(sig));
        RestTemplate client = new RestTemplate();


        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<byte[]> httpEntity = new HttpEntity<>(Start.MAPPER.writeValueAsBytes(t), headers);

        ResponseEntity<String> resp = client
                .exchange(System.getenv("URL"), HttpMethod.POST, httpEntity, String.class);
        System.out.println(resp.getBody());
    }

    public void testContractCall() throws Exception {
        HexBytes from = HexBytes.fromHex(TEST_PUBLIC_KEY);
        HexBytes to = Address.fromPublicKey(from.getBytes());
        HexBytes recipient = Address.of(TEST_RECIPIENT_PUBLIC_KEY);
        byte[] method = "transfer".getBytes(StandardCharsets.US_ASCII);
        Transaction t = Transaction.builder()
                .version(PoAConstants.TRANSACTION_VERSION)
                .createdAt(System.currentTimeMillis() / 1000)
                .from(from)
                .type(Transaction.Type.CONTRACT_CALL.code)
                .to(to)
                .payload(HexBytes.fromBytes(Bytes.concat(new byte[]{(byte) method.length},
                        method,
                        new byte[]{(byte)recipient.size()},
                        recipient.getBytes(),
                        LittleEndian.encodeInt64(1000)
                )))
                .build();

        byte[] sig = new Ed25519PrivateKey(HexBytes.fromHex(TEST_PRIVATE_KEY).getBytes()).sign(RLPCodec.encode(t));
        t.setSignature(HexBytes.fromBytes(sig));
        RestTemplate client = new RestTemplate();


        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<byte[]> httpEntity = new HttpEntity<>(Start.MAPPER.writeValueAsBytes(t), headers);

        ResponseEntity<String> resp = client
                .exchange(System.getenv("URL"), HttpMethod.POST, httpEntity, String.class);
        System.out.println(resp.getBody());
    }

}
