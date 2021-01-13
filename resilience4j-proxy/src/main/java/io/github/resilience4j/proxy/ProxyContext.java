package io.github.resilience4j.proxy;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.proxy.util.Reflect;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.RetryRegistry;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;

import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;

/**
 * Provides configurations for Resilience4jProxy decorators.
 */
public class ProxyContext {

    private final ConcurrentHashMap<Class<?>, Object> fallbacks = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduledExecutor = createDefaultScheduledExecutor();
    private RetryRegistry retryRegistry = RetryRegistry.ofDefaults();
    private RateLimiterRegistry rateLimiterRegistry = RateLimiterRegistry.ofDefaults();
    private CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();

    public ScheduledExecutorService getScheduledExecutorService() {
        return scheduledExecutor;
    }

    public void setRetryRegistry(RetryRegistry retryRegistry) {
        this.retryRegistry = retryRegistry;
    }

    public void setRateLimiterRegistry(RateLimiterRegistry rateLimiterRegistry) {
        this.rateLimiterRegistry = rateLimiterRegistry;
    }

    public void setCircuitBreakerRegistry(CircuitBreakerRegistry circuitBreakerRegistry) {
        this.circuitBreakerRegistry = circuitBreakerRegistry;
    }

    public RetryRegistry getRetryRegistry() {
        return retryRegistry;
    }

    public RateLimiterRegistry getRateLimiterRegistry() {
        return rateLimiterRegistry;
    }

    public CircuitBreakerRegistry getCircuitBreakerRegistry() {
        return circuitBreakerRegistry;
    }

    public <T> void addFallback(Class<T> key, T instance) {
        fallbacks.put(key, instance);
    }

    public <T> T lookupFallback(Class<T> key) {
        try {
            final Object result = fallbacks.computeIfAbsent(key, this::newInstance);
            return (T) result;
        } catch (ClassCastException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private <T> T newInstance(Class<T> clazz) {
        try {
            return Reflect.newInstance(clazz);
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    private ScheduledExecutorService createDefaultScheduledExecutor() {
        return newSingleThreadScheduledExecutor(r -> {
            final Thread thread = new Thread(r);
            thread.setDaemon(true);
            thread.setName("resilience4j-proxy-" + thread.getName());
            return thread;
        });
    }
}
