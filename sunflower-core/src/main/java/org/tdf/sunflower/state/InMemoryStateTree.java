package org.tdf.sunflower.state;

import org.tdf.common.util.ChainCache;
import org.tdf.common.types.Chained;
import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.exception.ExceptionUtil;
import org.tdf.sunflower.exception.StateUpdateException;
import org.tdf.sunflower.facade.StateTree;
import org.tdf.sunflower.types.Block;
import org.tdf.sunflower.types.ForkAbleState;
import org.tdf.sunflower.types.StateMap;

import java.util.*;
import java.util.stream.Collectors;

/**
 * State tree for account related object storage
 */
@Deprecated
// use trie instead
public class InMemoryStateTree<T extends ForkAbleState<T>> implements StateTree<T> {
    private StateMap<T> root;
    private ChainCache<StateMap<T>> cache;
    private T some;

    public InMemoryStateTree(Block genesis, Collection<? extends T> states) {
        if (states.size() == 0) throw new RuntimeException("at lease one states required");
        some = states.stream().findFirst().get();
        root = new StateMap<>(genesis.getHashPrev(), genesis.getHash(), states);
        cache = new ChainCache<>();
    }

    public void update(Block b) {
        if (cache.contains(b.getHash().getBytes())) return;
        if (b.getHeight() == 0) {
            throw new RuntimeException("please manually assign genesis states rather than update from empty");
        }
        if (!cache.contains(
                b.getHashPrev().getBytes()) &&
                !b.getHashPrev().equals(root.getHash())
        )
            throw new RuntimeException(
                "state sets not found at " + b.getHashPrev()
        );
        Set<String> all = new HashSet<>();
        b.getBody().stream().map(some::getIdentifiersOf).forEach(all::addAll);
        Map<String, T> states = all.stream()
                .map(id -> this.get(id, b.getHashPrev().getBytes()).orElse(some.createEmpty(id)))
                .collect(Collectors.toMap(ForkAbleState::getIdentifier, (s) -> s));
        states.values().forEach(s -> {
            try {
                s.update(b.getHeader());
            } catch (StateUpdateException e) {
                e.printStackTrace();
            }
        });
        for (T s : states.values()) {
            b.getBody().forEach(tx -> {
                try {
                    s.update(b, tx);
                } catch (StateUpdateException e) {
                    e.printStackTrace();
                }
            });
        }
        put(b, states.values());
    }

    // provide all already updated state
    public void put(Chained node, Collection<? extends T> allStates) {
        if (cache.contains(node.getHash().getBytes())) return;
        StateMap<T> forked = new StateMap<>(node.getHashPrev(), node.getHash(), allStates);
        cache.put(forked);
    }

    public Optional<T> get(String id, byte[] where) {
        if (this.root.getHash().equals(new HexBytes(where))) {
            // WARNING: do not use method reference here State::clone
            return root.get(id).map(s -> s.clone());
        }
        Optional<StateMap<T>> mapO = cache.get(where);
        if(!mapO.isPresent()) return Optional.empty();
        StateMap<T> map = mapO.get();
        Optional<T> o = map.get(id);
        if(o.isPresent()) return o;
        return get(id, map.getHashPrev().getBytes());
    }

    public T getLastConfirmed(String id) {
        return root.get(id).map(x -> x.clone()).orElse(some.createEmpty(id));
    }

    public void confirm(byte[] hash) {
        HexBytes h = new HexBytes(hash);
        if (root.getHash().equals(h)) return;
        List<StateMap<T>> children = cache.getChildren(root.getHash().getBytes());
        Optional<StateMap<T>> o = children.stream().filter(x -> x.getHash().equals(h)).findFirst();
        if (!o.isPresent()) {
            throw new RuntimeException("the state to confirm not found or confirmed block is not child of current node");
        }
        StateMap<T> map = o.get();
        children.stream().filter(x -> !x.getHash().equals(h))
                .forEach(n -> cache.removeDescendants(n.getHash().getBytes()));
        cache.remove(map.getHash().getBytes());
        for(String k: map.keySet()){
            root.put(k, map.get(k).orElseThrow(() -> ExceptionUtil.keyNotFound(k)));
        }
        root.setHash(map.getHash());
        root.setHashPrev(map.getHashPrev());
    }

    @Override
    public HexBytes getWhere() {
        return root.getHash();
    }
}
