package io.github.resilience4j.proxy.fallback;

import io.github.resilience4j.core.lang.Nullable;
import io.github.resilience4j.proxy.ProxyDecorator;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static io.github.resilience4j.proxy.util.AnnotationFinder.find;
import static io.github.resilience4j.proxy.util.Reflect.newInstance;

/**
 * Processes {@link io.github.resilience4j.proxy.fallback.Fallback} annotations and returns
 * a corresponding {@link io.github.resilience4j.proxy.ProxyDecorator}.
 */
public class FallbackProcessor {

    private final Map<Class<?>, Object> context = new ConcurrentHashMap<>();

    public FallbackProcessor(@Nullable Map<Class<?>, ?> instances) {
        if (instances != null) {
            context.putAll(instances);
        }
    }

    public Optional<ProxyDecorator> process(Method method) {
        final Fallback annotation = find(Fallback.class, method);

        if (annotation == null) {
            return Optional.empty();
        }

        final Object fallback = buildConfig(annotation);
        return Optional.of(new FallbackDecorator<>(new FallbackFactory<>(ex -> fallback)));
    }

    private Object buildConfig(Fallback annotation) {
        return newInstance(annotation.fallback(), context);
    }
}
