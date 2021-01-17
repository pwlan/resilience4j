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
package io.github.resilience4j.proxy;

import io.github.resilience4j.proxy.circuitbreaker.CircuitBreakerProcessor;
import io.github.resilience4j.proxy.exception.ExceptionMapper;
import io.github.resilience4j.proxy.exception.ExceptionsProcessor;
import io.github.resilience4j.proxy.fallback.FallbackProcessor;
import io.github.resilience4j.proxy.rateLimiter.RateLimiterProcessor;
import io.github.resilience4j.proxy.retry.RetryProcessor;
import io.vavr.CheckedFunction1;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Processes Resilience4jProxy annotations and decorates methods with the corresponding decorators.
 * The following decorators are supported and are applied in the following order: <br>
 * Retry <br>
 * RateLimiter<br>
 * CircuitBreaker<br>
 * Fallback<br>
 */
class AnnotationDecorator implements ProxyDecorator {

    private final RateLimiterProcessor rateLimiterProcessor;
    private final FallbackProcessor fallbackProcessor;
    private final RetryProcessor retryProcessor;
    private final CircuitBreakerProcessor circuitBreakerProcessor;
    private final ExceptionsProcessor exceptionsProcessor;

    AnnotationDecorator(ProxyContext context) {
        fallbackProcessor = new FallbackProcessor(context);
        retryProcessor = new RetryProcessor(context);
        rateLimiterProcessor = new RateLimiterProcessor(context);
        circuitBreakerProcessor = new CircuitBreakerProcessor(context);
        exceptionsProcessor = new ExceptionsProcessor(context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CheckedFunction1<Object[], ?> decorate(CheckedFunction1<Object[], ?> invocationCall, Method method) {
        final AtomicReference<CheckedFunction1<Object[], ?>> result = new AtomicReference<>(invocationCall);

        exceptionsProcessor.process(method).ifPresent(d -> result.set(d.decorate(invocationCall, method)));
        retryProcessor.process(method).ifPresent(d -> result.set(d.decorate(invocationCall, method)));
        rateLimiterProcessor.process(method).ifPresent(d -> result.set(d.decorate(invocationCall, method)));
        circuitBreakerProcessor.process(method).ifPresent(d -> result.set(d.decorate(invocationCall, method)));
        fallbackProcessor.process(method).ifPresent(d -> result.set(d.decorate(invocationCall, method)));

        return result.get();
    }
}
