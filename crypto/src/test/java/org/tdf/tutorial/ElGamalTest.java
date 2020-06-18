package org.tdf.tutorial;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.tdf.crypto.tutorial.ElgamalSigner;
import org.tdf.crypto.tutorial.SchnorrSigner;

import static org.junit.Assert.assertTrue;

@RunWith(JUnit4.class)
public class ElGamalTest {

    @Test
    public void test1() {
        ElgamalSigner signer = new ElgamalSigner(2, 467);
        long sk = 127;
        long x = 100;
        long beta = signer.getBeta(sk);
        long[] sig = signer.sign(sk, x);
        System.out.println(sig[0] + "  " + sig[1]);
        assertTrue(signer.verify(sig, x, beta));
    }

    @Test
    public void test2() {
        SchnorrSigner signer =
                new SchnorrSigner(7879, 101, 170);

        long sk = 75;
        long beta = signer.getBeta(sk);
        long x = 1000;
        long[] sig = signer.sign(sk, x);
        assertTrue(signer.verify(sig, 1000, beta));
    }
}
