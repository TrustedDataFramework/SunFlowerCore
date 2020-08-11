package org.tdf.sunflower.facade;


import org.tdf.common.util.HexBytes;

public interface Miner {
    void start();
    void stop();

    default HexBytes getMinerAddress(){
        return HexBytes.empty();
    }

    Miner NONE = new Miner() {
        @Override
        public void start() {

        }

        @Override
        public void stop() {

        }
    };
}
