package org.tdf.sunflower.controller;

import org.tdf.sunflower.types.TransactionInfo;
import org.tdf.sunflower.types.Block;

import static org.tdf.sunflower.controller.TypeConverter.toJsonHex;

public class TransactionReceiptDTOExt extends TransactionReceiptDTO {

    public String returnData;
    public String error;

    public TransactionReceiptDTOExt(Block block, TransactionInfo txInfo) {
        super(block, txInfo);
        returnData = toJsonHex(txInfo.getReceipt().getExecutionResult());
        error = txInfo.getReceipt().getError();
    }
}