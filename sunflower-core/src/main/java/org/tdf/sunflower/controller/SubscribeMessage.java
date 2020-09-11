package org.tdf.sunflower.controller;

import lombok.Data;
import org.tdf.common.util.HexBytes;

@Data
public class SubscribeMessage {
    private int code;
    private HexBytes data;
}
