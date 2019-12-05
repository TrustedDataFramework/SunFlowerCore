package org.tdf.sunflower.mq;

public interface Message {
    <T> T getAs(Class<T> clazz);
}
