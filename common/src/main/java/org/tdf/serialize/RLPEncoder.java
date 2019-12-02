package org.tdf.serialize;

public interface RLPEncoder<T> {
    RLPElement encode(T o);

    class None implements RLPEncoder{
        @Override
        public RLPElement encode(Object o) {
            return null;
        }
    }
}
