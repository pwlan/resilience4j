package io.github.resilience4j.proxy;

import io.github.resilience4j.proxy.bulkhead.BulkheadProcessor;
import io.github.resilience4j.proxy.circuitbreaker.CircuitBreakerProcessor;
import io.github.resilience4j.proxy.fallback.FallbackProcessor;
import io.github.resilience4j.proxy.rateLimiter.RateLimiterProcessor;
import io.github.resilience4j.proxy.retry.RetryProcessor;
import io.vavr.CheckedFunction1;

import java.lang.reflect.Method;

final class AnnotationDecorator implements ProxyDecorator {

    private final RateLimiterProcessor rateLimiterProcessor = new RateLimiterProcessor();
    private final FallbackProcessor fallbackProcessor = new FallbackProcessor();
    private final RetryProcessor retryProcessor = new RetryProcessor();
    private final CircuitBreakerProcessor circuitBreakerProcessor = new CircuitBreakerProcessor();
    private final BulkheadProcessor bulkheadProcessor = new BulkheadProcessor();

    @Override
    public CheckedFunction1<Object[], ?> decorate(CheckedFunction1<Object[], ?> invocationCall, Method method) {
        final ProxyDecorators.Builder builder = ProxyDecorators.builder();

        rateLimiterProcessor.process(method).ifPresent(builder::withRateLimiter);
        retryProcessor.process(method).ifPresent(r -> {
            if (r.isAsync()) {
                builder.withDefaultScheduledExecutor();
            }
            builder.withRetry(r.getRetry());
        });
        circuitBreakerProcessor.process(method).ifPresent(builder::withCircuitBreaker);
        fallbackProcessor.process(method).ifPresent(builder::withFallback);
        bulkheadProcessor.process(method).ifPresent(builder::withBulkhead);

        return builder.build().decorate(invocationCall, method);
    }
}
