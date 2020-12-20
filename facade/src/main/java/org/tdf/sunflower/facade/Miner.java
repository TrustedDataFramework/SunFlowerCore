package org.tdf.sunflower.facade;


import org.tdf.common.util.HexBytes;

public interface Miner {
    Miner NONE = new Miner() {
        @Override
        public void start() {

        }

        @Override
        public void stop() {

        }
    };

    void start();

    void stop();

    default HexBytes getMinerAddress() {
        return HexBytes.empty();
    }
}
