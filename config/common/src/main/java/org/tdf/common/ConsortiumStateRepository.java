package org.tdf.common;

import org.tdf.exception.StateUpdateException;
import org.tdf.serialize.SerializeDeserializer;

import java.util.*;

public class ConsortiumStateRepository implements StateRepository {
    private Map<Class<? extends State>, StateFactory> factories;

    private Map<Class<? extends ForkAbleState>, StateTree> trees;

    private Set<Class<? extends State>> classes = new HashSet<>();

    private boolean updated;

    private void assertRegisteredOnlyOnce(Class<? extends State> clazz) {
        if (classes.contains(clazz))
            throw new RuntimeException("the state " + clazz + " had been registered");
        classes.add(clazz);
    }

    private void assertRegistered(Class<? extends State> clazz){
        if (!classes.contains(clazz))
            throw new RuntimeException("the state " + clazz + " had not been registered");
    }

    private void assertNotUpdated(){
        if(updated) throw new RuntimeException("cannot register state after updated");
    }

    public Set<Class<? extends State>> getClasses() {
        return new HashSet<>(classes);
    }

    public ConsortiumStateRepository() {
        factories = new HashMap<>();
        trees = new HashMap<>();
    }

    @Override
    public <T extends State<T>> void register(Block genesis, T genesisState) throws StateUpdateException {
        assertRegisteredOnlyOnce(genesisState.getClass());
        assertNotUpdated();
        factories.put(genesisState.getClass(), new InMemoryStateFactory<>(genesis, genesisState));
    }

    @Override
    public <T extends ForkAbleState<T>> void register(Block genesis, Collection<? extends T> forkAbleStates) {
        assertNotUpdated();
        if (forkAbleStates.size() == 0) throw new RuntimeException("requires at least one state");
        T some = forkAbleStates.stream().findAny().get();
        assertRegisteredOnlyOnce(some.getClass());
        trees.put(
                some.getClass(),
                new InMemoryStateTree<>(genesis, forkAbleStates)
        );
    }

    @Override
    public <T extends State<T>> Optional<T> get(byte[] hash, Class<T> clazz) {
        assertRegistered(clazz);
        if (!factories.containsKey(clazz)) return Optional.empty();
        return (Optional<T>) factories.get(clazz).get(hash);
    }

    @Override
    public <T extends ForkAbleState<T>> Optional<T> get(String id, byte[] hash, Class<T> clazz) {
        assertRegistered(clazz);
        if (!trees.containsKey(clazz)) return Optional.empty();
        return (Optional<T>) trees.get(clazz).get(id, hash);
    }

    @Override
    public void update(Block b) {
        updated = true;
        factories.values().forEach(f -> f.update(b));
        trees.values().forEach(t -> t.update(b));
    }

    @Override
    public void put(Chained chained, State state) {
        updated = true;
        if (!factories.containsKey(state.getClass())) throw new RuntimeException(
                state.getClass().toString() + " has not been registered"
        );
        factories.get(state.getClass()).put(chained, state);
    }

    @Override
    public void put(Chained chained, Collection<ForkAbleState> forkAbleStates, Class<? extends ForkAbleState> clazz) {
        updated = true;
        if (!trees.containsKey(clazz)) throw new RuntimeException(
                clazz.toString() + " has not been registered"
        );
        trees.get(clazz).put(chained, forkAbleStates);
    }

    @Override
    public void confirm(byte[] hash) {
        updated = true;
        factories.values().forEach(f -> f.confirm(hash));
        trees.values().forEach(t -> t.confirm(hash));
    }

    @Override
    public <T extends State<T>> T getLastConfirmed(Class<T> clazz) {
        return (T) factories.get(clazz).getLastConfirmed();
    }

    @Override
    public <T extends ForkAbleState<T>> T getLastConfirmed(String id, Class<T> clazz) {
        return (T) trees.get(clazz).getLastConfirmed(id);
    }

    @Override
    public void onBlockWritten(Block block) {
        update(block);
    }

    @Override
    public void onNewBestBlock(Block block) {

    }

    @Override
    public void onBlockConfirmed(Block block) {
        confirm(block.getHash().getBytes());
    }

    public <T extends State<T>> void withPersistent(Class<T> clazz, Store<byte[], byte[]> store, SerializeDeserializer<T> serializeDeserializer){
        if(factories.containsKey(clazz)){
            ((InMemoryStateFactory<T>) factories.get(clazz)).withPersistent(store, serializeDeserializer);
            return;
        }
        if(trees.containsKey(clazz)){
            ((InMemoryStateTree) trees.get(clazz)).withPersistent(store, serializeDeserializer);
            return;
        }
        throw new RuntimeException(clazz + " had not been registered");
    }

    public <T extends State<T>> InMemoryStateFactory<T> getStateFactory(Class<T> clazz){
        assertRegistered(clazz);
        return (InMemoryStateFactory<T>) factories.get(clazz);
    }

    public <T extends ForkAbleState<T>> InMemoryStateTree<T> getStateTree(Class<T> clazz){
        assertRegistered(clazz);
        return (InMemoryStateTree<T>) trees.get(clazz);
    }
}
