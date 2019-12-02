package org.tdf.serialize;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;

public class RLPUtils {
    static RLPEncoder getAnnotatedRLPEncoder(AnnotatedElement element){
        if(!element.isAnnotationPresent(RLPEncoding.class)){
            return null;
        }
        Class<? extends RLPEncoder> encoder = element.getAnnotation(RLPEncoding.class).value();
        if(encoder == RLPEncoder.None.class){
           return null;
        }
        try {
            return encoder.newInstance();
        } catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    static RLPDecoder getAnnotatedRLPDecoder(AnnotatedElement element){
        if(!element.isAnnotationPresent(RLPDecoding.class)){
            return null;
        }
        Class<? extends RLPDecoder> decoder = element.getAnnotation(RLPDecoding.class).value();
        if(decoder == RLPDecoder.None.class){
            return null;
        }
        try {
            return decoder.newInstance();
        } catch (Exception e){
            throw new RuntimeException(e);
        }
    }

}
