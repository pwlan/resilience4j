package io.github.resilience4j.proxy.rateLimiter;

import io.github.resilience4j.core.lang.Nullable;
import io.github.resilience4j.proxy.Context;
import io.github.resilience4j.proxy.ProxyDecorator;
import io.github.resilience4j.proxy.rateLimiter.RateLimiter.None;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static io.github.resilience4j.proxy.util.AnnotationFinder.find;
import static io.github.resilience4j.proxy.util.Reflect.newInstance;

/**
 * Processes {@link io.github.resilience4j.proxy.rateLimiter.RateLimiter} annotations and returns
 * a corresponding {@link io.github.resilience4j.proxy.ProxyDecorator}.
 */
public class RateLimiterProcessor {

    private final Context context;

    public RateLimiterProcessor(Context context) {
        this.context = context;
    }

    public Optional<ProxyDecorator> process(Method method) {
        final io.github.resilience4j.proxy.rateLimiter.RateLimiter annotation =
            find(io.github.resilience4j.proxy.rateLimiter.RateLimiter.class, method);

        if (annotation == null) {
            return Optional.empty();
        }

        final RateLimiterConfig config = buildConfig(annotation);
        final RateLimiter rateLimiter = RateLimiter.of(annotation.name(), config);
        return Optional.of(new RateLimiterDecorator(rateLimiter));
    }

    private RateLimiterConfig buildConfig(io.github.resilience4j.proxy.rateLimiter.RateLimiter annotation) {
        final RateLimiterConfig.Builder config = RateLimiterConfig.custom();

        if (annotation.configProvider() != None.class) {
            return context.lookup(annotation.configProvider()).get();
        }

        if (annotation.limitForPeriod() != -1) {
            config.limitForPeriod(annotation.limitForPeriod());
        }

        return config.build();
    }
}
