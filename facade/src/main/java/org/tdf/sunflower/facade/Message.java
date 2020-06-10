package org.tdf.sunflower.facade;

public interface Message {
    <T> T as(Class<T> clazz);
}
