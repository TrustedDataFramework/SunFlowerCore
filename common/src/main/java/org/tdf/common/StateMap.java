package org.tdf.common;

import org.tdf.serialize.SerializeDeserializer;
import org.tdf.serialize.Serializers;
import org.tdf.store.MapStore;
import org.tdf.store.StoreWrapper;
import org.tdf.util.ExceptionUtil;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

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

    public StateMap<T> withPersistent(Store<byte[], byte[]> persistent, SerializeDeserializer<T> serializeDeserializer) {
        Store<String, T> tmp = new StoreWrapper<>(persistent, Serializers.STRING, serializeDeserializer);
        this.raw = new StoreWrapper<>(persistent, Serializers.STRING, Serializers.IDENTITY);
        for (T t : get().values()) {
            tmp.putIfAbsent(t.getIdentifier(), t);
        }
        this.data = tmp;
        raw.putIfAbsent(WHERE_PREFIX, super.getHash().getBytes());
        raw.putIfAbsent(HASH_PREV_PREFIX, super.getHashPrev().getBytes());
        return this;
    }

    @Override
    public HexBytes getHashPrev() {
        if (raw == null) return super.getHashPrev();
        return new HexBytes(
                raw.get(HASH_PREV_PREFIX).orElseThrow(() -> ExceptionUtil.keyNotFound(WHERE_PREFIX))
        );
    }

    @Override
    public HexBytes getHash() {
        if (raw == null) return super.getHash();
        return new HexBytes(
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

    public Set<String> keySet() {
        return get().keySet();
    }

    public Collection<T> values() {
        return data.values();
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
}
