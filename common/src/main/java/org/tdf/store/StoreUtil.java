package org.tdf.store;

import org.tdf.util.ExceptionUtil;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class StoreUtil {
    public static <K, V, R, U> Map<R, U> storeToMap(Store<K, V> store, Function<K, R> keyMap, Function<V, U> valueMap){
        return store.keySet().stream().collect(Collectors.toMap(
                keyMap, y -> valueMap.apply(store.get(y).orElseThrow(() -> ExceptionUtil.keyNotFound(y.toString()))))
        );
    }
}
