package io.github.resilience4j.proxy.reflect;

import io.github.resilience4j.core.lang.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

public final class AnnotationFinder {

    @Nullable
    public static <T extends Annotation> T find(Class<T> annotation, Method method) {
        final T methodAnnotation = method.getAnnotation(annotation);
        if(methodAnnotation != null) {
            return methodAnnotation;
        }
        return method.getDeclaringClass().getAnnotation(annotation);
    }
}
