package io.github.resilience4j.proxy.retry;

import io.github.resilience4j.core.lang.Nullable;
import io.github.resilience4j.proxy.fallback.Fallback;
import io.github.resilience4j.proxy.retry.Retry.None;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

import static io.github.resilience4j.proxy.reflect.AnnotationFinder.find;
import static io.github.resilience4j.proxy.reflect.Methods.isAsync;
import static io.github.resilience4j.proxy.reflect.Methods.newInstance;

public class RetryProcessor {

    private final Map<Class<?>, Object> predicates = new ConcurrentHashMap<>();

    public RetryProcessor(@Nullable Map<Class<?>, ?> instances) {
        if (instances != null) {
            predicates.putAll(instances);
        }
    }

    public Optional<Result> process(Method method) {
        final io.github.resilience4j.proxy.retry.Retry annotation =
            find(io.github.resilience4j.proxy.retry.Retry.class, method);

        if (annotation == null) {
            return Optional.empty();
        }

        final RetryConfig.Builder<?> config = RetryConfig.custom();

        if (annotation.retryExceptions().length != 0) {
            config.retryExceptions(annotation.retryExceptions());
        }
        if (annotation.maxAttempts() != -1) {
            config.maxAttempts(annotation.maxAttempts());
        }
        if (annotation.retryOnResult() != None.class) {
            config.retryOnResult(fetchPredicate(annotation.retryOnResult()));
        }
        if (annotation.retryOnException() != None.class) {
            config.retryOnException(fetchPredicate(annotation.retryOnException()));
        }
        if (annotation.waitDuration() != -1) {
            config.waitDuration(Duration.ofMillis(annotation.waitDuration()));
        }

        final Retry retry = Retry.of(annotation.name(), config.build());

        return Optional.of(new Result(retry, isAsync(method)));
    }

    private <T> Predicate<T> fetchPredicate(Class<? extends Predicate<?>> predicateClass) {
        final Object result = predicates.computeIfAbsent(predicateClass , key -> {
            try {
                return newInstance(predicateClass);
            } catch (Exception e) {
                throw new IllegalArgumentException("TODO");
            }
        });
        return (Predicate<T>) result;
    }

    public class Result {
        final private Retry retry;
        final private boolean async;

        public Result(Retry retry, boolean async) {
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
