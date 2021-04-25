package org.tdf.sunflower.state;

import org.tdf.sunflower.vm.Backend;
import org.tdf.sunflower.vm.CallData;


public interface PreBuiltContract extends CommonUpdater {
    default void update(
            Backend backend, CallData callData) {
    }
}
