package io.github.resilience4j.proxy.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;

public final class Reflect {

    private Reflect() {
    }

    public static <T> T newInstance(Class<T> instanceClass) throws NoSuchMethodException,
                                                                   IllegalAccessException,
                                                                   InvocationTargetException,
                                                                   InstantiationException {
        final Constructor<T> constructor = instanceClass.getDeclaredConstructor();
        constructor.setAccessible(true);
        return constructor.newInstance();
    }

    public static <T> T newInstance(Class<T> instanceClass, Map<Class<?>, Object> cache) {
        try {
            final Object result = cache.computeIfAbsent(instanceClass, key -> {
                try {
                    return newInstance(instanceClass);
                } catch (Exception e) {
                    throw new IllegalArgumentException("TODO");
                }
            });
            return (T) result;
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("TODO");
        }
    }

    public static Method getFallbackMethod(Object fallbackInstance, Method method) {
        final Method fallbackMethod;
        try {
            fallbackMethod = fallbackInstance.getClass()
                                             .getMethod(method.getName(), method.getParameterTypes());
        } catch (NoSuchMethodException | SecurityException e) {
            throw new IllegalArgumentException("Cannot use the fallback ["
                                               + fallbackInstance.getClass() + "] for ["
                                               + method.getDeclaringClass() + "]", e);
        }
        fallbackMethod.setAccessible(true);
        return fallbackMethod;
    }

    private static void validateFallback(Object fallbackInstance, Method method) {
        // TODO this is broken
        if (fallbackInstance.getClass().isAssignableFrom(method.getDeclaringClass())) {
            throw new IllegalArgumentException("Cannot use the fallback ["
                                               + fallbackInstance.getClass() + "] for ["
                                               + method.getDeclaringClass() + "]!");
        }
    }

    public static boolean isAsync(Method method) {
        return CompletionStage.class.isAssignableFrom(method.getReturnType());
    }
}
