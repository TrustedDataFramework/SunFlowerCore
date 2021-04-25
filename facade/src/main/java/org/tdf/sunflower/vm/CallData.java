package org.tdf.sunflower.vm;

import org.tdf.common.types.Uint256;
import org.tdf.common.util.HexBytes;

public interface CallData {
    HexBytes getCaller();

    Uint256 getAmount();

    Uint256 getTxAmount();

    HexBytes getTo();

    HexBytes getTxTo();

    int getTxType();

    int getCallType();

    HexBytes getPayload();

    HexBytes getOrigin();

    HexBytes getTxHash();

    long getTxNonce();

    HexBytes getTxSignature();

    long getTxCreatedAt();

    CallData clone();

    Uint256 getGasPrice();

    long getGasLimit();
}
