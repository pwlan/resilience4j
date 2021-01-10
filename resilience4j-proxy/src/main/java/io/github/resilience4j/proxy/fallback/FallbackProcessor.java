package io.github.resilience4j.proxy.fallback;

import io.github.resilience4j.core.lang.Nullable;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static io.github.resilience4j.proxy.util.AnnotationFinder.find;
import static io.github.resilience4j.proxy.util.Reflect.newInstance;

public class FallbackProcessor {

    private final Map<Class<?>, Object> fallbacks = new ConcurrentHashMap<>();

    public FallbackProcessor(@Nullable Map<Class<?>, ?> instances) {
        if (instances != null) {
            fallbacks.putAll(instances);
        }
    }

    public Optional<Object> process(Method method) {
        final Fallback annotation = find(Fallback.class, method);

        if (annotation == null) {
            return Optional.empty();
        }

        try {
            return Optional.of(fetchFallback(annotation));
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid Fallback!", e);
        }
    }

    private Object fetchFallback(Fallback annotation) {
        return fallbacks.computeIfAbsent(annotation.fallback(), key -> {
            try {
                final Object newFallback = newInstance(annotation.fallback());
                return newFallback;
            } catch (Exception e) {
                throw new IllegalArgumentException("TODO");
            }
        });
    }
}
