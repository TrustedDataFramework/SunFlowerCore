package org.tdf.sunflower.controller;

import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.types.Transaction;

public class TransactionInfo {
    private Status status;
    private HexBytes blockHash;
    private long height;
    private long confirms;
    private Transaction transaction;

    enum Status {
        PENDING,
        INCLUDED,
        DROPPED
    }
}
