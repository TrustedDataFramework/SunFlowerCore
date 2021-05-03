package org.tdf.sunflower.types;

import lombok.Value;
import org.tdf.common.util.HexBytes;

import java.util.List;

@Value
public class PendingData {
    List<Transaction> pending;
    List<TransactionReceipt> receipts;
    HexBytes trieRoot;
}
