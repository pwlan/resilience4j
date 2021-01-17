package io.github.resilience4j.proxy;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.proxy.exception.ExceptionMapper;
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

    private final ConcurrentHashMap<Class<?>, Object> beans = new ConcurrentHashMap<>();
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
     * @return the bean stored under the specified key. These can be fallbacks, exception mappers, a.s.o
     */
    public <T> T lookup(Class<T> key) {
        try {
            final Object result = beans.computeIfAbsent(key, this::newInstance);
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

    private <T> void addBean(Class<T> key, T instance) {
        beans.put(key, instance);
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

        /**
         * Adds a Fallback that can be used with {@link io.github.resilience4j.proxy.fallback.Fallback} annotations.
         * @param key must match the class specified in the {@link io.github.resilience4j.proxy.fallback.Fallback} annotation.
         * @param instance the Fallback. Can be any class or an instance of {@link io.github.resilience4j.proxy.fallback.FallbackHandler}.
         * @return the ProxyContext.Builder
         */
        public <T> Builder withFallback(Class<T> key, T instance) {
            proxyContext.addBean(key, instance);
            return this;
        }

        /**
         * Adds an ExceptionMapper that can be used with {@link io.github.resilience4j.proxy.exception.Exceptions} annotations.
         * @param key must match the class specified in the {@link io.github.resilience4j.proxy.exception.Exceptions} annotation.
         * @param instance the ExceptionMapper.
         * @return the ProxyContext.Builder
         */
        public <T extends ExceptionMapper> Builder withExceptionMapper(Class<T> key, T instance) {
            proxyContext.addBean(key, instance);
            return this;
        }

        /**
         * Adds a RetryRegistry that is used to resolve {@link io.github.resilience4j.retry.Retry}s.
         * @param registry the registry.
         * @return the ProxyContext.Builder
         */
        public Builder withRetryRegistry(RetryRegistry registry) {
            proxyContext.setRetryRegistry(registry);
            return this;
        }

        /**
         * Adds a CircuitBreakerRegistry that is used to resolve {@link io.github.resilience4j.circuitbreaker.CircuitBreaker}s.
         * @param registry the registry.
         * @return the ProxyContext.Builder
         */
        public Builder withCircuitBreakerRegistry(CircuitBreakerRegistry registry) {
            proxyContext.setCircuitBreakerRegistry(registry);
            return this;
        }

        /**
         * Adds a RateLimiterRegistry that is used to resolve {@link io.github.resilience4j.ratelimiter.RateLimiter}s.
         * @param registry the registry.
         * @return the ProxyContext.Builder
         */
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

        /**
         * Builds an instance of ProxyContext.
         *
         * @return the ProxyContext
         */
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
