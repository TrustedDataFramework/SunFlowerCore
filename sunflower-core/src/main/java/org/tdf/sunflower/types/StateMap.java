package org.tdf.sunflower.types;

import org.tdf.common.store.MapStore;
import org.tdf.common.store.Store;
import org.tdf.common.util.ChainedWrapper;
import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.exception.ExceptionUtil;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

public class StateMap<T extends ForkAbleState<T>> extends ChainedWrapper<Store<String, T>> implements Store<String, T> {
    private static final String WHERE_PREFIX = "where";
    private static final String HASH_PREV_PREFIX = "prev";
    private Store<String, byte[]> raw;

    public StateMap(HexBytes hashPrev, HexBytes hash, Collection<? extends T> states) {
        this.data = new MapStore<>();
        for (T t : states) {
            get().put(t.getIdentifier(), t);
        }
        super.setHashPrev(hashPrev);
        super.setHash(hash);
    }

    @Override
    public HexBytes getHashPrev() {
        if (raw == null) return super.getHashPrev();
        return HexBytes.fromBytes(
                raw.get(HASH_PREV_PREFIX).orElseThrow(() -> ExceptionUtil.keyNotFound(WHERE_PREFIX))
        );
    }

    @Override
    public HexBytes getHash() {
        if (raw == null) return super.getHash();
        return HexBytes.fromBytes(
                raw.get(WHERE_PREFIX).orElseThrow(() -> ExceptionUtil.keyNotFound(WHERE_PREFIX))
        );
    }

    @Override
    public void setHash(HexBytes hash) {
        if (raw == null) {
            super.setHash(hash);
            return;
        }
        raw.put(WHERE_PREFIX, hash.getBytes());
    }

    @Override
    public void setHashPrev(HexBytes hashPrev) {
        if (raw == null) {
            super.setHashPrev(hashPrev);
            return;
        }
        raw.put(HASH_PREV_PREFIX, hashPrev.getBytes());
    }

    private void assertNoConflict(String k) {
        if (k.equals(WHERE_PREFIX) || k.equals(HASH_PREV_PREFIX)) throw new RuntimeException(k + " conflicts");
    }

    public Optional<T> get(String s) {
        assertNoConflict(s);
        return get().get(s);
    }

    public void put(String s, T t) {
        assertNoConflict(s);
        data.put(s, t);
    }

    public void putIfAbsent(String s, T t) {
        assertNoConflict(s);
        data.putIfAbsent(s, t);
    }

    public void remove(String s) {
        assertNoConflict(s);
        data.remove(s);
    }

    public boolean containsKey(String s) {
        assertNoConflict(s);
        return data.containsKey(s);
    }

    public int size() {
        return data.size();
    }

    public boolean isEmpty() {
        return data.isEmpty();
    }

    public void clear() {
        data.clear();
    }

    @Override
    public void traverse(BiFunction<String, T, Boolean> traverser) {
        data.traverse(traverser);
    }

    @Override
    public void flush() {

    }

    @Override
    public Map<String, T> asMap() {
        throw new RuntimeException("not implemented");
    }

    @Override
    public void forEach(BiConsumer<String, T> consumer) {
        data.forEach(consumer);
    }
}
