package org.tdf.sunflower.facade;

import org.tdf.common.types.Chained;
import org.tdf.sunflower.exception.StateUpdateException;
import org.tdf.sunflower.types.Block;
import org.tdf.sunflower.types.ForkAbleState;
import org.tdf.sunflower.types.State;

import java.util.Collection;
import java.util.Optional;

@Deprecated // use state trie instead
public interface StateRepository extends ConsortiumRepositoryListener {
    <T extends State<T>> void register(Block genesis, T genesisState) throws StateUpdateException;

    <T extends ForkAbleState<T>> void register(Block genesis, Collection<? extends T> forkAbleStates);

    <T extends State<T>> Optional<T> get(byte[] hash, Class<T> clazz);

    <T extends ForkAbleState<T>> Optional<T> get(String id, byte[] hash, Class<T> clazz);

    <T extends State<T>> T getLastConfirmed(Class<T> clazz);

    <T extends ForkAbleState<T>> T getLastConfirmed(String id, Class<T> clazz);

    void update(Block b);

    void put(Chained chained, State state);

    void put(Chained chained, Collection<ForkAbleState> forkAbleStates, Class<? extends ForkAbleState> clazz);

    void confirm(byte[] hash);

    StateRepository NONE = new StateRepository() {
        @Override
        public <T extends State<T>> void register(Block genesis, T genesisState) throws StateUpdateException {

        }

        @Override
        public <T extends ForkAbleState<T>> void register(Block genesis, Collection<? extends T> forkAbleStates) {

        }

        @Override
        public <T extends State<T>> Optional<T> get(byte[] hash, Class<T> clazz) {
            return Optional.empty();
        }

        @Override
        public <T extends ForkAbleState<T>> Optional<T> get(String id, byte[] hash, Class<T> clazz) {
            return Optional.empty();
        }

        @Override
        public <T extends State<T>> T getLastConfirmed(Class<T> clazz) {
            return null;
        }

        @Override
        public <T extends ForkAbleState<T>> T getLastConfirmed(String id, Class<T> clazz) {
            return null;
        }

        @Override
        public void update(Block b) {

        }

        @Override
        public void put(Chained chained, State state) {

        }

        @Override
        public void put(Chained chained, Collection<ForkAbleState> forkAbleStates, Class<? extends ForkAbleState> clazz) {

        }

        @Override
        public void confirm(byte[] hash) {

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
