package org.tdf.sunflower.facade;


import org.tdf.sunflower.types.Block;

public interface Miner extends ConsortiumRepositoryListener {
    void start();
    void stop();
    void addListeners(MinerListener... listeners);

    Miner NONE = new Miner() {
        @Override
        public void start() {

        }

        @Override
        public void stop() {

        }

        @Override
        public void addListeners(MinerListener... listeners) {

        }

        @Override
        public void onBlockWritten(Block block) {

        }

        @Override
        public void onNewBestBlock(Block block) {

        }

        @Override
        public void onBlockConfirmed(Block block) {

        }
    };
}
