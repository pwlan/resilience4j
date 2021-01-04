package io.github.resilience4j.proxy.circuitbreaker;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import  io.github.resilience4j.circuitbreaker.CircuitBreaker;

import java.lang.reflect.Method;
import java.util.Optional;

import static io.github.resilience4j.proxy.reflect.AnnotationFinder.find;
import static java.time.Duration.ofMillis;

public class CircuitBreakerProcessor {

    public Optional<CircuitBreaker> process(Method method) {
        final io.github.resilience4j.proxy.circuitbreaker.CircuitBreaker annotation =
            find(io.github.resilience4j.proxy.circuitbreaker.CircuitBreaker.class, method);

        if (annotation == null) {
            return Optional.empty();
        }

        final CircuitBreakerConfig.Builder config = CircuitBreakerConfig.custom();

        if (annotation.slidingWindowSize() != -1) {
            config.slidingWindowSize(annotation.slidingWindowSize());
        }
        if (annotation.waitDurationInOpenState() != -1) {
            config.waitDurationInOpenState(ofMillis(annotation.waitDurationInOpenState()));
        }

       return Optional.of(CircuitBreaker.of(annotation.name(), config.build()));
    }
}
