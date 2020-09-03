package org.tdf.sunflower.vm.abi;

import lombok.Value;
import org.tdf.common.util.HexBytes;

@Value
public class ContextContract {
    HexBytes address;
    long nonce;
    HexBytes createdBy;
}
