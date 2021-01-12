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
package io.github.resilience4j.proxy.retry;

import io.github.resilience4j.proxy.Context;
import io.github.resilience4j.proxy.ProxyDecorator;
import io.github.resilience4j.proxy.retry.Retry.None;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Optional;

import static io.github.resilience4j.proxy.retry.RetryExecutor.getExecutorService;
import static io.github.resilience4j.proxy.util.AnnotationFinder.find;

/**
 * Processes {@link io.github.resilience4j.proxy.retry.Retry} annotations and returns
 * a corresponding {@link io.github.resilience4j.proxy.ProxyDecorator}.
 */
public class RetryProcessor {

    private final Context context;

    public RetryProcessor(Context context) {
        this.context = context;
    }

    public Optional<ProxyDecorator> process(Method method) {
        final io.github.resilience4j.proxy.retry.Retry annotation =
            find(io.github.resilience4j.proxy.retry.Retry.class, method);

        if (annotation == null) {
            return Optional.empty();
        }

        final RetryConfig config = buildConfig(annotation);
        final Retry retry = Retry.of(annotation.name(), config);
        return Optional.of(new RetryDecorator(retry, getExecutorService()));
    }

    private RetryConfig buildConfig(io.github.resilience4j.proxy.retry.Retry annotation) {
        final RetryConfig.Builder<?> config = RetryConfig.custom();

        if (annotation.configProvider() != None.class) {
            return context.lookup(annotation.configProvider()).get();
        }

        if (annotation.retryExceptions().length != 0) {
            config.retryExceptions(annotation.retryExceptions());
        }
        if (annotation.maxAttempts() != -1) {
            config.maxAttempts(annotation.maxAttempts());
        }
        if (annotation.retryOnException() != None.class) {
            config.retryOnException(context.lookup(annotation.retryOnException()));
        }
        if (annotation.waitDuration() != -1) {
            config.waitDuration(Duration.ofMillis(annotation.waitDuration()));
        }

        return config.build();
    }
}
