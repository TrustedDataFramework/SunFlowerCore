package org.tdf.sunflower.facade;


public interface Miner {
    void start();
    void stop();

    Miner NONE = new Miner() {
        @Override
        public void start() {

        }

        @Override
        public void stop() {

        }
    };
}
