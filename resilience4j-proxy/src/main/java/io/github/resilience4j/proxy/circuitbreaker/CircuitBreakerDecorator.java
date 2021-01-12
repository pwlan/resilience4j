/*
 *
 * Copyright 2021
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
package io.github.resilience4j.proxy.circuitbreaker;

import io.github.resilience4j.proxy.ProxyDecorator;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.vavr.CheckedFunction1;

import java.lang.reflect.Method;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;

import static io.github.resilience4j.circuitbreaker.CircuitBreaker.decorateCheckedFunction;
import static io.github.resilience4j.circuitbreaker.CircuitBreaker.decorateCompletionStage;
import static io.github.resilience4j.proxy.util.Functions.toSupplier;
import static io.github.resilience4j.proxy.util.Reflect.isAsync;
import static io.github.resilience4j.ratelimiter.RateLimiter.decorateCheckedFunction;
import static io.github.resilience4j.ratelimiter.RateLimiter.decorateCompletionStage;

/**
 * Implementation of {@link ProxyDecorator} that decorates functions with a RateLimiter.
 */
class CircuitBreakerDecorator implements ProxyDecorator {

    private final CircuitBreaker circuitBreaker;

    CircuitBreakerDecorator(CircuitBreaker circuitBreaker) {
        this.circuitBreaker = circuitBreaker;
    }

    @Override
    public CheckedFunction1<Object[], ?> decorate(CheckedFunction1<Object[], ?> fn, Method method) {
        if (isAsync(method)) {
            return (args) -> decorateCompletionStage(circuitBreaker, toSupplier(fn, args)).get();
        }
        return decorateCheckedFunction(circuitBreaker, fn);
    }
}
