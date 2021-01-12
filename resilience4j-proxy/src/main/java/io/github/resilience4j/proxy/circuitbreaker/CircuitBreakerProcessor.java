package io.github.resilience4j.proxy.circuitbreaker;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.core.lang.Nullable;
import io.github.resilience4j.proxy.ProxyDecorator;
import io.github.resilience4j.proxy.circuitbreaker.CircuitBreaker.None;
import io.github.resilience4j.proxy.rateLimiter.RateLimiter;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static io.github.resilience4j.proxy.util.AnnotationFinder.find;
import static io.github.resilience4j.proxy.util.Reflect.newInstance;
import static java.time.Duration.ofMillis;

/**
 * Processes {@link io.github.resilience4j.proxy.circuitbreaker.CircuitBreaker} annotations and returns
 * a corresponding {@link io.github.resilience4j.proxy.ProxyDecorator}.
 */
public class CircuitBreakerProcessor {

    private final Map<Class<?>, Object> context = new ConcurrentHashMap<>();

    public CircuitBreakerProcessor(@Nullable Map<Class<?>, ?> instances) {
        if (instances != null) {
            context.putAll(instances);
        }
    }

    public Optional<ProxyDecorator> process(Method method) {
        final io.github.resilience4j.proxy.circuitbreaker.CircuitBreaker annotation =
            find(io.github.resilience4j.proxy.circuitbreaker.CircuitBreaker.class, method);

        if (annotation == null) {
            return Optional.empty();
        }

        final CircuitBreakerConfig config = buildConfig(annotation);
        final CircuitBreaker circuitBreaker = CircuitBreaker.of(annotation.name(), config);
        return Optional.of(new CircuitBreakerDecorator(circuitBreaker));
    }

    private CircuitBreakerConfig buildConfig(io.github.resilience4j.proxy.circuitbreaker.CircuitBreaker annotation) {
        final CircuitBreakerConfig.Builder config = CircuitBreakerConfig.custom();

        if (annotation.configProvider() != None.class) {
            return newInstance(annotation.configProvider(), context).get();
        }

        if (annotation.slidingWindowSize() != -1) {
            config.slidingWindowSize(annotation.slidingWindowSize());
        }
        if (annotation.waitDurationInOpenState() != -1) {
            config.waitDurationInOpenState(ofMillis(annotation.waitDurationInOpenState()));
        }

        return config.build();
    }
}
