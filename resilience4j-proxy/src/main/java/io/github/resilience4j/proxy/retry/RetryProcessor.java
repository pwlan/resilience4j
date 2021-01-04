package io.github.resilience4j.proxy.retry;

import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;

import java.lang.reflect.Method;
import java.util.Optional;

import static io.github.resilience4j.proxy.reflect.AnnotationFinder.find;
import static io.github.resilience4j.proxy.reflect.Methods.isAsync;

public class RetryProcessor {

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

        final Retry retry = Retry.of(annotation.name(), config.build());

        return Optional.of(new Result(retry, isAsync(method)));
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
