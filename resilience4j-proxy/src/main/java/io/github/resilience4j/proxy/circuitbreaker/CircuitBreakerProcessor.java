/*
 * Copyright 2021
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.resilience4j.proxy.circuitbreaker;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.proxy.Context;
import io.github.resilience4j.proxy.ProxyDecorator;
import io.github.resilience4j.proxy.circuitbreaker.CircuitBreaker.None;

import java.lang.reflect.Method;
import java.util.Optional;

import static io.github.resilience4j.proxy.util.AnnotationFinder.find;
import static java.time.Duration.ofMillis;

/**
 * Processes {@link io.github.resilience4j.proxy.circuitbreaker.CircuitBreaker} annotations and returns
 * a corresponding {@link io.github.resilience4j.proxy.ProxyDecorator}.
 */
public class CircuitBreakerProcessor {

    private final Context context;

    public CircuitBreakerProcessor(Context context) {
        this.context = context;
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
            return context.lookup(annotation.configProvider()).get();
        }

        if (annotation.slidingWindowSize() != -1) {
            config.slidingWindowSize(annotation.slidingWindowSize());
        }
        if (annotation.waitDurationInOpenState() != -1) {
            config.waitDurationInOpenState(ofMillis(annotation.waitDurationInOpenState()));
        }
        if (annotation.failureRateThreshold() != -1) {
            config.failureRateThreshold(annotation.failureRateThreshold());
        }

        return config.build();
    }
}
