package org.tdf.common.types;

import org.tdf.common.util.HexBytes;

import java.util.*;
import java.util.stream.Collectors;

public interface Chained extends Hashed {
    HexBytes getHashPrev();

    default boolean isParentOf(Chained another) {
        return another.isChildOf(this);
    }

    default boolean isChildOf(Chained another) {
        return isChildOf(another.getHash());
    }

    default boolean isChildOf(HexBytes hash) {
        return getHashPrev().equals(hash);
    }

    static <T extends Chained> List<T> getInitialsOf(Collection<T> col) {
        Set<T> s = new TreeSet<>(Comparator.comparing(Hashed::getHash));
        s.addAll(col);
        s.removeIf(t -> col.stream().anyMatch(x -> x.isParentOf(t)));

        return new ArrayList<>(s);
    }

    static <T extends Chained> List<T> getLeavesOf(Collection<T> col) {
        Set<T> s = new TreeSet<>(Comparator.comparing(Hashed::getHash));
        s.addAll(col);
        s.removeIf(t -> col.stream().anyMatch(x -> x.isChildOf(t)));
        return new ArrayList<>(s);
    }

    static <T extends Chained> List<T> getFork(Collection<T> col, HexBytes leaf) {
        List<T> ret = getAncestorsOf(col, leaf);
        col.stream()
                .filter(x -> x.getHash().equals(leaf))
                .findAny()
                .ifPresent(ret::add);
        return ret;
    }

    static <T extends Chained> List<List<T>> getForks(Collection<T> col) {
        return getLeavesOf(col).stream().map(c -> getFork(col, c.getHash())).collect(Collectors.toList());
    }

    static <T extends Chained> List<T> getDescendentsOf(Collection<T> col, HexBytes hash) {
        Deque<T> stack = new LinkedList<>();
        Set<T> ret = new TreeSet<>(Comparator.comparing(Hashed::getHash));

        col.stream()
                .filter(x -> x.isChildOf(hash) || x.getHash().equals(hash))
                .forEach(stack::add);

        while (!stack.isEmpty()) {
            T first = stack.poll();
            ret.add(first);
            col.stream().filter(x -> x.isChildOf(first))
                    .forEach(stack::add);
        }

        ret.removeIf(x -> x.getHash().equals(hash));
        return new ArrayList<>(ret);
    }

    static <T extends Chained> List<T> getAncestorsOf(Collection<T> col, HexBytes hash) {
        Map<HexBytes, T> m = new HashMap<>();
        for (T t : col) {
            m.put(t.getHash(), t);
        }
        List<T> li = new ArrayList<>();
        T t = m.get(hash);
        while (t != null) {
            li.add(t);
            t = m.get(t.getHashPrev());
        }
        li.removeIf(x -> x.getHash().equals(hash));
        Collections.reverse(li);
        return li;
    }
}
