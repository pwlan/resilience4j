package io.github.resilience4j.proxy.rateLimiter;

import io.github.resilience4j.proxy.ProxyContext;
import io.github.resilience4j.proxy.ProxyDecorator;
import io.github.resilience4j.ratelimiter.RateLimiter;

import java.lang.reflect.Method;
import java.util.Optional;

import static io.github.resilience4j.proxy.util.AnnotationFinder.find;

/**
 * Processes {@link io.github.resilience4j.proxy.rateLimiter.RateLimiter} annotations and returns
 * a corresponding {@link io.github.resilience4j.proxy.ProxyDecorator}.
 */
public class RateLimiterProcessor {

    private final ProxyContext context;

    public RateLimiterProcessor(ProxyContext context) {
        this.context = context;
    }

    public Optional<ProxyDecorator> process(Method method) {
        final io.github.resilience4j.proxy.rateLimiter.RateLimiter annotation =
            find(io.github.resilience4j.proxy.rateLimiter.RateLimiter.class, method);

        if (annotation == null) {
            return Optional.empty();
        }

        final RateLimiter rateLimiter = buildRateLimiter(annotation);
        return Optional.of(new RateLimiterDecorator(rateLimiter));
    }

    private RateLimiter buildRateLimiter(io.github.resilience4j.proxy.rateLimiter.RateLimiter annotation) {
        return context.getRateLimiterRegistry().rateLimiter(annotation.name());
    }
}
