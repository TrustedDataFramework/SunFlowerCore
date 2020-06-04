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
        Map<HexBytes, T> m = new HashMap<>();
        for (T t : col) {
            m.put(t.getHash(), t);
        }
        List<T> li = new ArrayList<>();
        T t = m.get(leaf);
        while (t != null) {
            li.add(t);
            t = m.get(t.getHashPrev());
        }
        Collections.reverse(li);
        return li;
    }

    static <T extends Chained> List<List<T>> getForks(Collection<T> col) {
        return getLeavesOf(col).stream().map(c -> getFork(col, c.getHash())).collect(Collectors.toList());
    }

    static <T extends Chained> List<T> getDescendentsOf(Collection<T> col, HexBytes hash) {
        Set<T> s = new TreeSet<>(Comparator.comparing(Hashed::getHash));
        s.addAll(col);

        Set<HexBytes> ret = new HashSet<>();
        ret.add(hash);

        while (true) {
            int size = ret.size();
            Iterator<T> it = s.iterator();
            while (it.hasNext()) {
                T t = it.next();
                if (ret.stream().anyMatch(t::isChildOf)) {
                    ret.add(t.getHash());
                    it.remove();
                }
            }
            if (ret.size() == size)
                break;
        }


        Map<HexBytes, T> m = new HashMap<>();
        for (T t : col) {
            m.put(t.getHash(), t);
        }
        ret.remove(hash);
        return ret
                .stream()
                .map(m::get)
                .peek(Objects::requireNonNull)
                .collect(Collectors.toList());
    }

    static <T extends Chained> List<T> getAncestorsOf(Collection<T> col, HexBytes hash) {

        Set<T> s = new TreeSet<>(Comparator.comparing(Hashed::getHash));
        s.addAll(col);

        Set<T> ret = new TreeSet<>(Comparator.comparing(Hashed::getHash));

        Optional<T> o = col.stream()
                .filter(x -> x.getHash().equals(hash))
                .findAny();

        o.ifPresent(ret::add);


        while (true) {
            int size = ret.size();
            Iterator<T> it = s.iterator();
            while (it.hasNext()) {
                T t = it.next();
                if (ret.stream().anyMatch(t::isParentOf)) {
                    ret.add(t);
                    it.remove();
                }
            }
            if (ret.size() == size)
                break;
        }

        o.ifPresent(ret::remove);
        return new ArrayList<>(ret);
    }
}
