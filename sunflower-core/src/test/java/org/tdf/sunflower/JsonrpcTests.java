package org.tdf.sunflower;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.tdf.sunflower.controller.TypeConverter;

@RunWith(JUnit4.class)
public class JsonrpcTests {

    @Test
    public void test0() {
        System.out.println(TypeConverter.toJsonHex(0));
    }
}
