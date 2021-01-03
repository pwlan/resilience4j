package io.github.resilience4j.proxy.annotations;

import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.proxy.ProxyDecorator;
import io.github.resilience4j.proxy.ProxyDecorators;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.retry.RetryConfig;
import io.vavr.CheckedFunction1;

import java.lang.reflect.Method;
import java.util.concurrent.CompletionStage;

import static io.github.resilience4j.proxy.reflect.AnnotationFinder.find;
import static java.time.Duration.ofMillis;

public final class AnnotationDecorator implements ProxyDecorator {

    @Override
    public CheckedFunction1<Object[], ?> decorate(CheckedFunction1<Object[], ?> invocationCall, Method method) {
        final ProxyDecorators.Builder builder = ProxyDecorators.builder();
        findRateLimiter(method, builder);
        findRetry(method, builder);
        findCircuitBreaker(method, builder);
        findFallback(method, builder);
        findBulkhead(method, builder);
        return builder.build().decorate(invocationCall, method);
    }

    private void findRetry(Method method, ProxyDecorators.Builder builder) {
        final Retry annotation = find(Retry.class, method);

        if (annotation == null) {
            return;
        }

        final RetryConfig.Builder config = RetryConfig.custom();

        if (annotation.retryExceptions().length != 0) {
            config.retryExceptions(annotation.retryExceptions());
        }
        if (annotation.maxAttempts() != -1) {
            config.maxAttempts(annotation.maxAttempts());
        }
        final io.github.resilience4j.retry.Retry retry =
            io.github.resilience4j.retry.Retry.of(annotation.name(), config.build());
        if (isAsync(method)) {
            builder.withDefaultScheduledExecutor();
        }

        builder.withRetry(retry);
    }

    private void findCircuitBreaker(Method method, ProxyDecorators.Builder builder) {
        final CircuitBreaker annotation = find(CircuitBreaker.class, method);

        if (annotation == null) {
            return;
        }

        final CircuitBreakerConfig.Builder config = CircuitBreakerConfig.custom();

        if (annotation.slidingWindowSize() != -1) {
            config.slidingWindowSize(annotation.slidingWindowSize());
        }
        if (annotation.waitDurationInOpenState() != -1) {
            config.waitDurationInOpenState(ofMillis(annotation.waitDurationInOpenState()));
        }

        final io.github.resilience4j.circuitbreaker.CircuitBreaker circuitBreaker =
            io.github.resilience4j.circuitbreaker.CircuitBreaker.of(annotation.name(), config.build());
        builder.withCircuitBreaker(circuitBreaker);
    }

    private void findRateLimiter(Method method, ProxyDecorators.Builder builder) {
        final RateLimiter annotation = find(RateLimiter.class, method);

        if (annotation == null) {
            return;
        }

        final RateLimiterConfig.Builder config = RateLimiterConfig.custom();

        // TODO add config

        final io.github.resilience4j.ratelimiter.RateLimiter rateLimiter =
            io.github.resilience4j.ratelimiter.RateLimiter.of(annotation.name(), config.build());
        builder.withRateLimiter(rateLimiter);
    }

    private void findBulkhead(Method method, ProxyDecorators.Builder builder) {
        final Bulkhead annotation = find(Bulkhead.class, method);

        if (annotation == null) {
            return;
        }

        final BulkheadConfig.Builder config = BulkheadConfig.custom();

        // TODO add config

        final io.github.resilience4j.bulkhead.Bulkhead bulkhead =
            io.github.resilience4j.bulkhead.Bulkhead.of(annotation.name(), config.build());
        builder.withBulkhead(bulkhead);
    }

    private void findFallback(Method method, ProxyDecorators.Builder builder) {
        final Fallback annotation = find(Fallback.class, method);

        if (annotation == null) {
            return;
        }

        try {
            builder.withFallback(annotation.fallback().getDeclaredConstructor().newInstance());
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid Fallback!", e);
        }
    }

    private boolean isAsync(Method method) {
        return CompletionStage.class.isAssignableFrom(method.getReturnType());
    }
}
