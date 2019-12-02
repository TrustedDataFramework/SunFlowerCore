package org.tdf.consortium.consensus.vrf.contract;

import java.math.BigInteger;

/**
 * @author James Hu
 * @since 2019/5/21
 */
public class DepositData {

    private byte[] vrfPk;
    private BigInteger deposit;

    public DepositData(byte[] vrfPk, BigInteger deposit) {
        this.vrfPk = vrfPk;
        this.deposit = deposit;
    }

    public byte[] getVrfPk() {
        return vrfPk;
    }

    public BigInteger getDeposit() {
        return deposit;
    }

    public void setVrfPk(byte[] vrfPk) {
        this.vrfPk = vrfPk;
    }

    public void setDeposit(BigInteger deposit) {
        this.deposit = deposit;
    }
}