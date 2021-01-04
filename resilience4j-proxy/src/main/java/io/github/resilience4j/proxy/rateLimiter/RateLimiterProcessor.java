package io.github.resilience4j.proxy.rateLimiter;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;

import java.lang.reflect.Method;
import java.util.Optional;

import static io.github.resilience4j.proxy.reflect.AnnotationFinder.find;

public class RateLimiterProcessor {

    public Optional<RateLimiter> process(Method method) {
        final io.github.resilience4j.proxy.rateLimiter.RateLimiter annotation =
            find(io.github.resilience4j.proxy.rateLimiter.RateLimiter.class, method);

        if (annotation == null) {
            return Optional.empty();
        }

        if (annotation.provider() != NoProvider.class) {
            try {
                return Optional.of(annotation.provider().getDeclaredConstructor().newInstance().get());
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid RateLimiter Provider!", e);
            }
        }

        final RateLimiterConfig.Builder config = RateLimiterConfig.custom();
        if (annotation.limitForPeriod() != -1) {
            config.limitForPeriod(annotation.limitForPeriod());
        }

        return Optional.of(RateLimiter.of(annotation.name(), config.build()));
    }
}
