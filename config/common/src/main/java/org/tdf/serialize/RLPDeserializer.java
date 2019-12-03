package org.tdf.serialize;

import org.tdf.util.ReflectionUtil;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class RLPDeserializer<T> implements Deserializer<T> {
    public static RLPDeserializer<RLPElement> DESERIALIZER = new RLPDeserializer<>(RLPElement.class);

    private Class<T> clazz;

    public RLPDeserializer(Class<T> clazz) {
        this.clazz = clazz;
    }

    public static <T> T deserialize(byte[] data, Class<T> clazz) {
        RLPElement element = RLPElement.fromEncoded(data);
        return deserialize(element, clazz);
    }

    public static <T> List<T> deserializeList(byte[] data, Class<T> elementType) {
        RLPElement element = RLPElement.fromEncoded(data);
        return deserializeList(element.getAsList(), elementType);
    }

    private static <T> List<T> deserializeList(RLPList list, Class<T> elementType) {
        if (elementType == RLPElement.class) return (List<T>) list;
        if (elementType == RLPItem.class) {
            return (List<T>) list.stream().map(x -> x.getAsItem()).collect(Collectors.toList());
        }
        List<T> res = new ArrayList<>(list.size());
        for (int i = 0; i < list.size(); i++) {
            res.add(deserialize(list.get(i), elementType));
        }
        return res;
    }

    public static <T> T deserialize(RLPElement element, Class<T> clazz) {
        if (clazz == RLPElement.class) return (T) element;
        if (clazz == RLPList.class) return (T) element.getAsList();
        if (clazz == RLPItem.class) return (T) element.getAsItem();
        RLPDecoder decoder = RLPUtils.getAnnotatedRLPDecoder(clazz);
        if (decoder != null) return (T) decoder.decode(element);
        // non null terminals
        if (clazz == Byte.class || clazz == byte.class) {
            return (T) Byte.valueOf(element.getAsItem().getByte());
        }
        if (clazz == Short.class || clazz == short.class) {
            return (T) Short.valueOf(element.getAsItem().getShort());
        }
        if (clazz == Integer.class || clazz == int.class) {
            return (T) Integer.valueOf(element.getAsItem().getInt());
        }
        if (clazz == Long.class || clazz == long.class) {
            return (T) Long.valueOf(element.getAsItem().getLong());
        }
        if (clazz == byte[].class) {
            return (T) element.getAsItem().get();
        }
        if (element.isNull()) return null;
        if (clazz == BigInteger.class) {
            return (T) element.getAsItem().getBigInteger();
        }
        if (clazz == String.class) {
            return (T) element.getAsItem().getString();
        }
        if (clazz.isArray()) {
            Class elementType = clazz.getComponentType();
            Object res = Array.newInstance(clazz.getComponentType(), element.getAsList().size());
            for (int i = 0; i < element.getAsList().size(); i++) {
                Array.set(res, i, deserialize(element.getAsList().get(i), elementType));
            }
            return (T) res;
        }
        // cannot determine generic type at runtime
        if (clazz == List.class) {
            return (T) deserializeList(element.getAsList(), RLPElement.class);
        }
        Object o;
        try {
            o = clazz.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        List<Field> fields = ReflectionUtil.getRLPFields(clazz);
        if (fields.size() == 0) throw new RuntimeException(clazz + " is not supported not RLP annotation found");
        for (int i = 0; i < fields.size(); i++) {
            RLPElement el = element.getAsList().get(i);
            Field f = fields.get(i);
            f.setAccessible(true);
            RLPDecoder fieldDecoder = RLPUtils.getAnnotatedRLPDecoder(f);
            if (fieldDecoder != null) {
                try {
                    f.set(o, fieldDecoder.decode(el));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                continue;
            }
            if (!f.getType().equals(List.class)) {
                try {
                    f.set(o, deserialize(el, f.getType()));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                continue;
            }
            if (!f.isAnnotationPresent(ElementType.class)) {
                throw new RuntimeException("field " + f + " require an ElementType annotation");
            }
            Class elementType = f.getAnnotation(ElementType.class).value();


            try {
                if (el.isNull()) {
                    f.set(o, null);
                    continue;
                }
                f.set(o, deserializeList(el.getAsList(), elementType));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return (T) o;
    }

    @Override
    public T deserialize(byte[] data) {
        RLPElement element = RLPElement.fromEncoded(data);
        if (clazz == RLPElement.class) return (T) element;
        return deserialize(element, clazz);
    }
}
