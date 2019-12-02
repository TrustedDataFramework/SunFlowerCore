package org.wisdom.consortium.vm.wasm;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.wisdom.consortium.vm.wasm.runtime.Register;

@RunWith(JUnit4.class)
public class RegisterTest {

    @Test
    public void testPushFloat(){
        Register s = new Register(0);
        s.pushF32(0.5f);
        assert s.getData().length == 1;
        assert s.popF32() == 0.5f;
    }
    
}
