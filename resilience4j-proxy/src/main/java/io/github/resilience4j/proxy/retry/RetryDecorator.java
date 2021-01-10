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
package io.github.resilience4j.proxy.retry;

import io.github.resilience4j.core.lang.Nullable;
import io.github.resilience4j.proxy.ProxyDecorator;
import io.github.resilience4j.retry.Retry;
import io.vavr.CheckedFunction1;

import java.lang.reflect.Method;
import java.util.concurrent.ScheduledExecutorService;

import static io.github.resilience4j.proxy.util.Functions.toSupplier;
import static io.github.resilience4j.proxy.util.Reflect.isAsync;
import static io.github.resilience4j.retry.Retry.decorateCheckedFunction;
import static io.github.resilience4j.retry.Retry.decorateCompletionStage;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.Executors.newScheduledThreadPool;

/**
 * Implementation of {@link ProxyDecorator} that decorates functions with a Retry.
 */
class RetryDecorator implements ProxyDecorator {

    private final ScheduledExecutorService scheduledExecutor;
    private final Retry retry;

    RetryDecorator(Retry retry, @Nullable ScheduledExecutorService scheduledExecutor) {
        this.retry = retry;
        this.scheduledExecutor = scheduledExecutor;
    }

    @Override
    public CheckedFunction1<Object[], ?> decorate(CheckedFunction1<Object[], ?> fn, Method method) {
        if (isAsync(method)) {
            return (args) -> {
                requireNonNull(scheduledExecutor, "scheduledExecutor");
                return decorateCompletionStage(retry, scheduledExecutor, toSupplier(fn, args)).get();
            };
        }
        return decorateCheckedFunction(retry, fn);
    }
}
