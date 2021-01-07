package io.github.resilience4j.proxy;

import io.github.resilience4j.core.lang.Nullable;
import io.github.resilience4j.proxy.circuitbreaker.CircuitBreakerProcessor;
import io.github.resilience4j.proxy.fallback.FallbackProcessor;
import io.github.resilience4j.proxy.rateLimiter.RateLimiterProcessor;
import io.github.resilience4j.proxy.retry.RetryProcessor;
import io.vavr.CheckedFunction1;

import java.lang.reflect.Method;
import java.util.Map;

final class AnnotationDecorator implements ProxyDecorator {

    private final RateLimiterProcessor rateLimiterProcessor;
    private final FallbackProcessor fallbackProcessor;
    private final RetryProcessor retryProcessor;
    private final CircuitBreakerProcessor circuitBreakerProcessor = new CircuitBreakerProcessor();

    public <T> AnnotationDecorator(@Nullable Map<Class<?>, ?> context) {
        // TODO validate instances
        fallbackProcessor = new FallbackProcessor(context);
        retryProcessor = new RetryProcessor(context);
        rateLimiterProcessor = new RateLimiterProcessor(context);
    }

    public AnnotationDecorator() {
        this(null);
    }

    @Override
    public CheckedFunction1<Object[], ?> decorate(CheckedFunction1<Object[], ?> invocationCall, Method method) {
        final ProxyDecorators.Builder builder = ProxyDecorators.builder();

        retryProcessor.process(method).ifPresent(r -> {
            if (r.isAsync()) {
                builder.withDefaultScheduledExecutor();
            }
            builder.withRetry(r.getRetry());
        });
        fallbackProcessor.process(method).ifPresent(builder::withFallback);
        rateLimiterProcessor.process(method).ifPresent(builder::withRateLimiter);
        circuitBreakerProcessor.process(method).ifPresent(builder::withCircuitBreaker);

        return builder.build().decorate(invocationCall, method);
    }
}
