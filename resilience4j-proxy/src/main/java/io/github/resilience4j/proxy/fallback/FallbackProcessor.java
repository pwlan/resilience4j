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
package io.github.resilience4j.proxy.fallback;

import io.github.resilience4j.core.lang.Nullable;
import io.github.resilience4j.proxy.ProxyDecorator;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static io.github.resilience4j.proxy.util.AnnotationFinder.find;
import static io.github.resilience4j.proxy.util.Reflect.newInstance;

/**
 * Processes {@link io.github.resilience4j.proxy.fallback.Fallback} annotations and returns
 * a corresponding {@link io.github.resilience4j.proxy.ProxyDecorator}.
 */
public class FallbackProcessor {

    private final Map<Class<?>, Object> context = new ConcurrentHashMap<>();

    public FallbackProcessor(@Nullable Map<Class<?>, Object> instances) {
        if (instances != null) {
            context.putAll(instances);
        }
    }

    public Optional<ProxyDecorator> process(Method method) {
        final Fallback annotation = find(Fallback.class, method);

        if (annotation == null) {
            return Optional.empty();
        }

        final FallbackHandler fallbackHandler = buildConfig(annotation);
        return Optional.of(new FallbackDecorator(fallbackHandler));
    }

    private FallbackHandler buildConfig(Fallback annotation) {
        final Object fallback = newInstance(annotation.fallback(), context);

        if (fallback instanceof FallbackHandler) {
            return (FallbackHandler) fallback;
        }
        return new DefaultFallbackHandler(fallback);
    }
}
