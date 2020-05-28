package org.tdf.crypto.keystore;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.tdf.common.util.HexBytes;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class CipherParams {
    private HexBytes iv;
}
