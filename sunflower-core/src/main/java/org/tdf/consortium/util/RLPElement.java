package org.tdf.consortium.util;

import java.io.Serializable;

/**
 * Wrapper class for decoded elements from an RLP encoded byte array.
 *
 * @author Roman Mandeleil
 * @since 01.04.2014
 */
public interface RLPElement extends Serializable {

    byte[] getRLPBytes();

    String getRLPHexString();

    String getRLPString();

    int getRLPInt();

    byte getRLPByte();

    long getRLPLong();
}
