/*
 *
 * Copyright 2020
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 *
 *
 */
package io.github.resilience4j.proxy;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.proxy.fallback.FallbackDecorator;
import io.github.resilience4j.proxy.fallback.FallbackFactory;
import io.github.resilience4j.proxy.reflect.Methods;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.retry.Retry;
import io.vavr.CheckedFunction1;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Supplier;

import static io.github.resilience4j.proxy.reflect.Methods.isAsync;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.Executors.newScheduledThreadPool;

/**
 * Builder to help build stacked decorators. <br>
 *
 * <pre>
 * {
 *     &#64;code
 *     CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("backendName");
 *     RateLimiter rateLimiter = RateLimiter.ofDefaults("backendName");
 *     FeignDecorators decorators = FeignDecorators.builder()
 *             .withCircuitBreaker(circuitBreaker)
 *             .withRateLimiter(rateLimiter)
 *             .build();
 *     MyService myService = Resilience4jFeign.builder(decorators).target(MyService.class, "http://localhost:8080/");
 * }
 * </pre>
 * <p>
 * The order in which decorators are applied correspond to the order in which they are declared. For
 * example, calling before {@link
 * Builder#withCircuitBreaker(CircuitBreaker)} would mean that the fallback is
 * called when the HTTP request fails, but would no longer be reachable if the CircuitBreaker were
 * open. However, reversing the order would mean that the fallback is called both when the HTTP
 * request fails and when the CircuitBreaker is open. <br> So be wary of this when designing your
 * "resilience" strategy.
 */
final class ProxyDecorators implements ProxyDecorator {

    private final List<ProxyDecorator> decorators;

    private ProxyDecorators(List<ProxyDecorator> decorators) {
        this.decorators = decorators;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public CheckedFunction1<Object[], ?> decorate(CheckedFunction1<Object[], ?> fn, Method method) {
        CheckedFunction1<Object[], ?> decoratedFn = fn;
        for (final ProxyDecorator decorator : decorators) {
            decoratedFn = decorator.decorate(decoratedFn, method);
        }
        return decoratedFn;
    }

    public static final class Builder {

        private final List<ProxyDecorator> decorators = new ArrayList<>();
        private ScheduledExecutorService scheduledExecutor;

        /**
         * TODO
         */
        public Builder withDefaultScheduledExecutor() {
            scheduledExecutor = newScheduledThreadPool(10, r -> {
                final Thread thread = new Thread(r);
                thread.setDaemon(true);
                thread.setName("resilience4j-" + thread.getName());
                return thread;
            });
            return this;
        }

        /**
         * TODO
         */
        public Builder withScheduledExecutor(ScheduledExecutorService scheduledExecutor) {
            this.scheduledExecutor = scheduledExecutor;
            return this;
        }

        /**
         * Adds a {@link Retry} to the decorator chain.
         *
         * @param retry a fully configured {@link Retry}.
         * @return the builder
         */
        public Builder withRetry(Retry retry) {
            decorators.add((fn, m) -> {
                if (isAsync(m)) {
                    return (args) -> {
                        requireNonNull(scheduledExecutor, "scheduledExecutor");
                        return Retry.decorateCompletionStage(retry,
                                                             scheduledExecutor,
                                                             toSupplier(fn, args)).get();
                    };
                }
                return Retry.decorateCheckedFunction(retry, fn);
            });
            return this;
        }

        /**
         * Adds a {@link CircuitBreaker} to the decorator chain.
         *
         * @param circuitBreaker a fully configured {@link CircuitBreaker}.
         * @return the builder
         */
        public Builder withCircuitBreaker(CircuitBreaker circuitBreaker) {
            decorators.add((fn, m) -> {
                if (isAsync(m)) {
                    return (args) -> CircuitBreaker.decorateCompletionStage(circuitBreaker,
                                                                            toSupplier(fn, args)).get();
                }
                return CircuitBreaker.decorateCheckedFunction(circuitBreaker, fn);
            });
            return this;
        }

        /**
         * Adds a {@link RateLimiter} to the decorator chain.
         *
         * @param rateLimiter a fully configured {@link RateLimiter}.
         * @return the builder
         */
        public Builder withRateLimiter(RateLimiter rateLimiter) {
            decorators.add((fn, m) -> {
                if (isAsync(m)) {
                    return (args) -> RateLimiter.decorateCompletionStage(rateLimiter, toSupplier(fn, args)).get();
                }
                return RateLimiter.decorateCheckedFunction(rateLimiter, fn);
            });
            return this;
        }

        /**
         * Adds a {@link Bulkhead} to the decorator chain.
         *
         * @param bulkhead a fully configured {@link Bulkhead}.
         * @return the builder
         */
        public Builder withBulkhead(Bulkhead bulkhead) {
            decorators.add((fn, m) -> Bulkhead.decorateCheckedFunction(bulkhead, fn));
            return this;
        }

        /**
         * Adds a fallback to the decorator chain. Multiple fallbacks can be applied with the next
         * fallback being called when the previous one fails.
         *
         * @param fallback TODO
         * @return the builder
         */
        public ProxyDecorators.Builder withFallback(Object fallback) {
            decorators.add(new FallbackDecorator<>(new FallbackFactory<>(ex -> fallback)));
            return this;
        }

        /**
         * Builds the decorator chain.
         *
         * @return the decorators.
         */
        public ProxyDecorators build() {
            return new ProxyDecorators(decorators);
        }

        /**
         * TODO
         */
        private <R> Supplier<CompletionStage<R>> toSupplier(CheckedFunction1<Object[], R> function, Object[] args) {
            return () -> {
                try {
                    return (CompletionStage<R>) function.apply(args);
                } catch (Throwable throwable) {
                    throw new RuntimeException(throwable);
                }
            };
        }
    }
}
