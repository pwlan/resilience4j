package io.github.resilience4j.proxy;

import io.github.resilience4j.core.lang.Nullable;
import io.github.resilience4j.proxy.circuitbreaker.CircuitBreakerProcessor;
import io.github.resilience4j.proxy.fallback.FallbackProcessor;
import io.github.resilience4j.proxy.rateLimiter.RateLimiterProcessor;
import io.github.resilience4j.proxy.retry.RetryProcessor;
import io.vavr.CheckedFunction1;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

final class AnnotationDecorator implements ProxyDecorator {

    private final RateLimiterProcessor rateLimiterProcessor;
    private final FallbackProcessor fallbackProcessor;
    private final RetryProcessor retryProcessor;
    private final CircuitBreakerProcessor circuitBreakerProcessor;

    public <T> AnnotationDecorator(@Nullable Map<Class<?>, ?> context) {
        // TODO validate instances
        fallbackProcessor = new FallbackProcessor(context);
        retryProcessor = new RetryProcessor(context);
        rateLimiterProcessor = new RateLimiterProcessor(context);
        circuitBreakerProcessor =  new CircuitBreakerProcessor(context);
    }

    public AnnotationDecorator() {
        this(null);
    }

    @Override
    public CheckedFunction1<Object[], ?> decorate(CheckedFunction1<Object[], ?> invocationCall, Method method) {
        CheckedFunction1<Object[], ?> result = invocationCall;

        retryProcessor.process(method).ifPresent(d -> d.decorate(invocationCall, method));
        rateLimiterProcessor.process(method).ifPresent(d -> d.decorate(invocationCall, method));
        circuitBreakerProcessor.process(method).ifPresent(d -> d.decorate(invocationCall, method));

        return result;
    }
}
