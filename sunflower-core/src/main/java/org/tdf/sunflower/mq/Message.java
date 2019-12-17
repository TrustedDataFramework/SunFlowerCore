package org.tdf.sunflower.mq;

public interface Message {
    <T> T as(Class<T> clazz);
}
