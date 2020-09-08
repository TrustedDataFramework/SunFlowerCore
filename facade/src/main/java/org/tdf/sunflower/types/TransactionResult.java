package org.tdf.sunflower.types;

import lombok.Value;

import java.util.Map;

@Value
public class TransactionResult {
    private byte[]
    private Map<String, byte[]> events;
}
