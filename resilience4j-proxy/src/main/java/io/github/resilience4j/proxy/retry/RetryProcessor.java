package io.github.resilience4j.proxy.retry;

import io.github.resilience4j.core.lang.Nullable;
import io.github.resilience4j.proxy.retry.Retry.None;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static io.github.resilience4j.proxy.reflect.AnnotationFinder.find;
import static io.github.resilience4j.proxy.reflect.Methods.isAsync;
import static io.github.resilience4j.proxy.reflect.Methods.newInstance;

public class RetryProcessor {

    private final Map<Class<?>, Object> context = new ConcurrentHashMap<>();

    public RetryProcessor(@Nullable Map<Class<?>, ?> instances) {
        if (instances != null) {
            context.putAll(instances);
        }
    }

    public Optional<RetryProcessorResult> process(Method method) {
        final io.github.resilience4j.proxy.retry.Retry annotation =
            find(io.github.resilience4j.proxy.retry.Retry.class, method);

        if (annotation == null) {
            return Optional.empty();
        }

        final RetryConfig config = buildConfig(annotation);
        return Optional.of(new RetryProcessorResult(Retry.of(annotation.name(), config), isAsync(method)));
    }

    private RetryConfig buildConfig(io.github.resilience4j.proxy.retry.Retry annotation) {
        final RetryConfig.Builder<?> config = RetryConfig.custom();

        if (annotation.configProvider() != None.class) {
            return instance(annotation.configProvider()).get();
        }

        if (annotation.retryExceptions().length != 0) {
            config.retryExceptions(annotation.retryExceptions());
        }
        if (annotation.maxAttempts() != -1) {
            config.maxAttempts(annotation.maxAttempts());
        }
        if (annotation.retryOnException() != None.class) {
            config.retryOnException(instance(annotation.retryOnException()));
        }
        if (annotation.waitDuration() != -1) {
            config.waitDuration(Duration.ofMillis(annotation.waitDuration()));
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

    public static class RetryProcessorResult {
        final private Retry retry;
        final private boolean async;

        public RetryProcessorResult(Retry retry, boolean async) {
            this.retry = retry;
            this.async = async;
        }

        public Retry getRetry() {
            return retry;
        }

        public boolean isAsync() {
            return async;
        }
    }
}
