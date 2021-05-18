package org.tdf.sunflower.types;

import lombok.RequiredArgsConstructor;
import org.tdf.common.store.Store;
import org.tdf.common.util.HexBytes;
import org.tdf.common.util.RLPUtil;
import org.tdf.rlpstream.Rlp;
import org.tdf.rlpstream.RlpList;

import java.util.*;

@RequiredArgsConstructor
public class StorageWrapper {
    private final HexBytes prefix;
    private final Store<HexBytes, HexBytes> store;


    public StorageWrapper(Store<HexBytes, HexBytes> store) {
        this(HexBytes.empty(), store);
    }

    private HexBytes verifyAndPrefix(HexBytes key) {
        return prefix.concat(key);
    }

    public Set<HexBytes> keySet() {
        if (this.prefix.isEmpty())
            throw new UnsupportedOperationException();
        HexBytes keys = store.get(prefix);
        return keys == null || keys.size() == 0 ?
            new TreeSet<>() :
            new TreeSet<>(
                Arrays.asList(RLPUtil.decode(keys, HexBytes[].class))
            );
    }


    private void addKey(HexBytes key) {
        Set<HexBytes> keySet = keySet();
        keySet.add(key);
        store.set(prefix, HexBytes.fromBytes(Rlp.encode(keySet)));
    }

    public void save(HexBytes key, Object o) {
        HexBytes prefixed = verifyAndPrefix(key);
        // if this is prefix store, add key
        if (!this.prefix.isEmpty()) {
            addKey(key);
        }
        store.set(prefixed, RLPUtil.encode(o));
    }

    private void removeKey(HexBytes key) {
        Set<HexBytes> keySet = keySet();
        keySet.remove(key);
        store.set(prefix, HexBytes.fromBytes(Rlp.encode(keySet)));
    }

    public void remove(HexBytes key) {
        HexBytes prefixed = verifyAndPrefix(key);
        // if this is prefix store, add key
        if (!this.prefix.isEmpty()) {
            removeKey(key);
        }
        store.remove(prefixed);
    }

    public <T> T get(HexBytes key, Class<T> clazz, T defaultValue) {
        HexBytes prefixed = verifyAndPrefix(key);
        HexBytes h = store.get(prefixed);
        if (h == null)
            return defaultValue;
        return RLPUtil.decode(h, clazz);
    }

    public <T> List<T> getList(HexBytes key, Class<T> clazz, List<T> defaultValue) {
        HexBytes prefixed = verifyAndPrefix(key);
        HexBytes h = store.get(prefixed);
        if (h == null)
            return defaultValue;
        List<T> ret = new ArrayList<>();
        RlpList li = Rlp.decodeList(h.getBytes());

        for(int i = 0; i < li.size(); i++) {
            ret.add(
                Rlp.decode(li.rawAt(i), clazz)
            );
        }
        return ret;
    }

    public <T extends Comparable<T>> Set<T> getSet(HexBytes key, Class<T> clazz, Set<T> defaultValue) {
        HexBytes prefixed = verifyAndPrefix(key);
        HexBytes h = store.get(prefixed);
        if (h == null)
            return defaultValue;
        TreeSet<T> ret = new TreeSet<>();

        RlpList li = Rlp.decodeList(h.getBytes());

        for(int i = 0; i < li.size(); i++) {
            ret.add(
                Rlp.decode(li.rawAt(i), clazz)
            );
        }
        return ret;
    }
}
