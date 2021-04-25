package org.tdf.sunflower.state;

import org.tdf.sunflower.vm.Backend;

public interface Bios extends CommonUpdater {
    default void update(
            Backend backend) {
        throw new IllegalArgumentException();
    }
}
