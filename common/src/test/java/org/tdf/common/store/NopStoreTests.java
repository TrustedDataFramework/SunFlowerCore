package org.tdf.common.store;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class NopStoreTests {
    @Test
    public void test0(){
        Store<String, String> nop = Store.getNop();
        nop.put("k", "v");
        assert !nop.containsKey("k");
        assert nop.isEmpty();
    }
}
