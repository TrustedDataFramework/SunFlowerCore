package org.tdf.sunflower.types;

import lombok.AllArgsConstructor;
import org.tdf.common.util.HexBytes;

@AllArgsConstructor
public class WebSocketTransactionBody {
    private HexBytes hash;
    private int status;
    private Object data;
}
