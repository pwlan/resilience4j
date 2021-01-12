package io.github.resilience4j.proxy;

import io.github.resilience4j.proxy.util.Reflect;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Provides instances of classes that are defined in Resilience4jProxy annotations.
 */
public class Context {

    final ConcurrentHashMap<Class<?>, Object> lookup = new ConcurrentHashMap<>();

    public <T> void add(Class<T> key, T instance) {
        lookup.put(key, instance);
    }

    public <T> T lookup(Class<T> key) {
        try {
            final Object result = lookup.computeIfAbsent(key, this::newInstance);
            return (T) result;
        } catch (ClassCastException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private <T> T newInstance(Class<T> clazz) {
        try {
            return Reflect.newInstance(clazz);
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }
}
