package io.github.resilience4j.proxy;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.proxy.util.Reflect;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.RetryRegistry;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;

import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;

/**
 * Provides configurations for <code>Resilience4jProxy</code>s.
 *
 * <pre>{@code
 *   ProxyContext context = ProxyContext.builder()
 *       .withRetryRegistry(RetryRegistry.ofDefaults())
 *       .withCircuitBreakerRegistry(CircuitBreakerRegistry.ofDefaults())
 *       .build();
 *   Resilience4jProxy resilience4jProxy = Resilience4jProxy.build(context);
 *   }</pre>
 */
public class ProxyContext {

    private final ConcurrentHashMap<Class<?>, Object> fallbacks = new ConcurrentHashMap<>();
    private ScheduledExecutorService scheduledExecutor;
    private RetryRegistry retryRegistry;
    private RateLimiterRegistry rateLimiterRegistry;
    private CircuitBreakerRegistry circuitBreakerRegistry;

    private ProxyContext() {
        // called by the ProxyContext.Builder
    }

    public ScheduledExecutorService getScheduledExecutorService() {
        return scheduledExecutor;
    }

    /**
     * @return the specified {@link RetryRegistry} or the default RetryRegistry.ofDefaults().
     */
    public RetryRegistry getRetryRegistry() {
        return retryRegistry;
    }

    /**
     * @return the specified {@link RateLimiterRegistry} or the default RateLimiterRegistry.ofDefaults().
     */
    public RateLimiterRegistry getRateLimiterRegistry() {
        return rateLimiterRegistry;
    }

    /**
     * @return the specified {@link CircuitBreakerRegistry} or the default CircuitBreakerRegistry.ofDefaults().
     */
    public CircuitBreakerRegistry getCircuitBreakerRegistry() {
        return circuitBreakerRegistry;
    }

    /**
     * @return the specified fallback or creates a new instance of the class passed as the key parameter.
     * For this, the class must have a no-arg constructor.
     */
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

    /**
     * @return the {@link Builder} used to create new instances {@link ProxyContext}.
     */
    public static Builder builder() {
        return new Builder();
    }

    private <T> void addFallback(Class<T> key, T instance) {
        fallbacks.put(key, instance);
    }

    private void setRetryRegistry(RetryRegistry retryRegistry) {
        this.retryRegistry = retryRegistry;
    }

    private void setRateLimiterRegistry(RateLimiterRegistry rateLimiterRegistry) {
        this.rateLimiterRegistry = rateLimiterRegistry;
    }

    private void setCircuitBreakerRegistry(CircuitBreakerRegistry circuitBreakerRegistry) {
        this.circuitBreakerRegistry = circuitBreakerRegistry;
    }

    private void setScheduledExecutorService(ScheduledExecutorService scheduledExecutor) {
        this.scheduledExecutor = scheduledExecutor;
    }

    public static final class Builder {

        private final ProxyContext proxyContext = new ProxyContext();

        private Builder() {
        }

        public <T> Builder withFallback(Class<T> key, T instance) {
            proxyContext.addFallback(key, instance);
            return this;
        }

        public Builder withRetryRegistry(RetryRegistry registry) {
            proxyContext.setRetryRegistry(registry);
            return this;
        }

        public Builder withCircuitBreakerRegistry(CircuitBreakerRegistry registry) {
            proxyContext.setCircuitBreakerRegistry(registry);
            return this;
        }

        public Builder withRateLimiterRegistry(RateLimiterRegistry registry) {
            proxyContext.setRateLimiterRegistry(registry);
            return this;
        }

        private ScheduledExecutorService createDefaultScheduledExecutor() {
            return newSingleThreadScheduledExecutor(r -> {
                final Thread thread = new Thread(r);
                thread.setDaemon(true);
                thread.setName("resilience4j-proxy-" + thread.getName());
                return thread;
            });
        }

        public ProxyContext build() {
            if (proxyContext.getScheduledExecutorService() == null) {
                proxyContext.setScheduledExecutorService(createDefaultScheduledExecutor());
            }
            if (proxyContext.getCircuitBreakerRegistry() == null) {
                proxyContext.setCircuitBreakerRegistry(CircuitBreakerRegistry.ofDefaults());
            }
            if (proxyContext.getRateLimiterRegistry() == null) {
                proxyContext.setRateLimiterRegistry(RateLimiterRegistry.ofDefaults());
            }
            if (proxyContext.getRetryRegistry() == null) {
                proxyContext.setRetryRegistry(RetryRegistry.ofDefaults());
            }
            return proxyContext;
        }
    }
}
