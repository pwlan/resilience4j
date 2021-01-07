package io.github.resilience4j.proxy.rateLimiter;

import io.github.resilience4j.core.lang.Nullable;
import io.github.resilience4j.proxy.rateLimiter.RateLimiter.None;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static io.github.resilience4j.proxy.reflect.AnnotationFinder.find;
import static io.github.resilience4j.proxy.reflect.Methods.newInstance;

public class RateLimiterProcessor {

    private final Map<Class<?>, Object> context = new ConcurrentHashMap<>();

    public RateLimiterProcessor(@Nullable Map<Class<?>, ?> instances) {
        if (instances != null) {
            context.putAll(instances);
        }
    }

    public Optional<RateLimiter> process(Method method) {
        final io.github.resilience4j.proxy.rateLimiter.RateLimiter annotation =
            find(io.github.resilience4j.proxy.rateLimiter.RateLimiter.class, method);

        if (annotation == null) {
            return Optional.empty();
        }

        final RateLimiterConfig config = buildConfig(annotation);
        return Optional.of(RateLimiter.of(annotation.name(), config));
    }

    private RateLimiterConfig buildConfig(io.github.resilience4j.proxy.rateLimiter.RateLimiter annotation) {
        final RateLimiterConfig.Builder config = RateLimiterConfig.custom();

        if (annotation.configProvider() != None.class) {
            return instance(annotation.configProvider()).get();
        }

        if (annotation.limitForPeriod() != -1) {
            config.limitForPeriod(annotation.limitForPeriod());
        }

        return config.build();
    }

    private <T> T instance(Class<T> instanceClass) {
        try {
            final Object result = context.computeIfAbsent(instanceClass, key -> {
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
}
