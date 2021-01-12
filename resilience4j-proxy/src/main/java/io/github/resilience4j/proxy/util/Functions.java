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
package io.github.resilience4j.proxy.util;

import io.github.resilience4j.core.lang.Nullable;
import io.vavr.CheckedFunction1;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

public final class Functions {

    private Functions() {
    }

    public static  <R> Supplier<CompletionStage<R>> toSupplier(CheckedFunction1<Object[], R> function, Object[] args) {
        return () -> {
            try {
                return (CompletionStage<R>) function.apply(args);
            } catch (Throwable throwable) {
                throw new RuntimeException(throwable);
            }
        };
    }
}
