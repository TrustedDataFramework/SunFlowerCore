package org.tdf.common.util;

import org.tdf.common.types.Chained;

import java.util.*;

public interface ChainCache<T extends Chained> extends SortedSet<T> {

    /**
     * a thread safe chain cache
     */
    int CONCURRENT_LEVEL_ONE = 1;

    class Builder<T extends Chained> {
        private int maximumSize;

        private int concurrentLevel;

        private Comparator<? super T> comparator = (Comparator<T>) (o1, o2) -> {
            if (o1.getHash().equals(o2.getHashPrev())) return -1;
            if (o1.getHashPrev().equals(o2.getHash())) return 1;
            return o1.getHash().compareTo(o2.getHash());
        };

        public Builder<T> maximumSize(int maximumSize) {
            this.maximumSize = maximumSize;
            return this;
        }

        public Builder<T> comparator(Comparator<? super T> comparator) {
            this.comparator = comparator;
            return this;
        }

        public Builder<T> concurrentLevel(int concurrentLevel){
            this.concurrentLevel = concurrentLevel;
            return this;
        }

        public ChainCache<T> build() {
            ChainCacheImpl<T> ret = new ChainCacheImpl<>(maximumSize, comparator);
            if(concurrentLevel == 0) return ret;
            return ConcurrentChainCache.of(ret);
        }
    }

    static <T extends Chained> Builder<T> builder(){
        return new Builder<>();
    }

    static <T extends Chained> ChainCache<T> of(Collection<T> nodes) {
        return ChainCacheImpl.of(nodes);
    }

    Optional<T> get(byte[] hash);

    List<T> getDescendants(byte[] hash);

    List<List<T>> getAllForks();

    void removeDescendants(byte[] hash);

    List<T> getLeaves();

    List<T> getInitials();

    boolean removeByHash(byte[] hash);

    List<T> popLongestChain();

    boolean containsHash(byte[] hash);

    List<T> getAncestors(byte[] hash);

    List<T> getChildren(byte[] hash);

}
