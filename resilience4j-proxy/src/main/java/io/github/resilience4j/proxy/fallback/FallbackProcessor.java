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

import io.github.resilience4j.proxy.ProxyContext;
import io.github.resilience4j.proxy.ProxyDecorator;

import java.lang.reflect.Method;
import java.util.Optional;

import static io.github.resilience4j.proxy.util.AnnotationFinder.find;

/**
 * Processes {@link io.github.resilience4j.proxy.fallback.Fallback} annotations and returns
 * a corresponding {@link io.github.resilience4j.proxy.ProxyDecorator}.
 */
public class FallbackProcessor {

    private final ProxyContext context;

    public FallbackProcessor(ProxyContext context) {
       this.context = context;
    }

    public Optional<ProxyDecorator> process(Method method) {
        final Fallback annotation = find(Fallback.class, method);

        if (annotation == null) {
            return Optional.empty();
        }

        final FallbackHandler fallbackHandler = buildFallback(annotation);
        return Optional.of(new FallbackDecorator(fallbackHandler));
    }

    private FallbackHandler buildFallback(Fallback annotation) {
        final Object fallback = context.lookupFallback(annotation.fallback());

        if (fallback instanceof FallbackHandler) {
            return (FallbackHandler) fallback;
        }
        return new DefaultFallbackHandler(fallback);
    }
}
