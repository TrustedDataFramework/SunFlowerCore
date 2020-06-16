package org.tdf.crypto.keystore;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.tdf.common.util.HexBytes;
import org.tdf.gmhelper.SM3Util;

import java.util.Arrays;

@NoArgsConstructor
@Data
@AllArgsConstructor
public class KeyStoreImpl {
    static final int SALT_LENGTH = 32;
    static final int IV_LENGTH = 16;
    static final String DEFAULT_VERSION = "1";

    private HexBytes publicKey;
    private Crypto crypto;
    private String id;
    private String version;
    private HexBytes mac;
    private String kdf;

    @JsonIgnore
    private HexBytes privateKey;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public HexBytes getAddress() {
        byte[] d = SM3Util.hash(publicKey.getBytes());
        byte[] ret = Arrays.copyOfRange(d, d.length - 20, d.length);
        return HexBytes.fromBytes(ret);
    }
}
