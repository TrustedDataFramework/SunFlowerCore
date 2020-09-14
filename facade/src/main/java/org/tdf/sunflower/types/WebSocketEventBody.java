package org.tdf.sunflower.types;

import lombok.AllArgsConstructor;
import org.tdf.rlp.RLPList;

@AllArgsConstructor
public class WebSocketEventBody {
    private byte[] address;
    private String name;
    private RLPList outputs;
}
