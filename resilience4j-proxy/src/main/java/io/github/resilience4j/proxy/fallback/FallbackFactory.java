/*
 *
 * Copyright 2019
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
package io.github.resilience4j.proxy.fallback;

import io.github.resilience4j.proxy.reflect.Methods;
import io.vavr.CheckedFunction1;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * TODO
 */
public class FallbackFactory<T> {

    private final Function<Exception, T> fallbackSupplier;

    public FallbackFactory(Function<Exception, T> fallbackSupplier) {
        this.fallbackSupplier = fallbackSupplier;
    }

    public CheckedFunction1<Object[], ?> decorate(CheckedFunction1<Object[], ?> invocationCall,
                                                  Method method,
                                                  Predicate<Exception> filter) {
        return args -> {
            try {
                final Object result = invocationCall.apply(args);
                if (result instanceof CompletionStage) {
                    return handleCompletionStage((CompletionStage<?>) result,
                                                 method,
                                                 args,
                                                 filter);
                }
                return result;
            } catch (Exception exception) {
                if (filter.test(exception)) {
                    return executeFallback(method, exception, args);
                }
                throw exception;
            }
        };
    }

    private Object executeFallback(Method method, Exception exception, Object[] args) throws Throwable {
        final T fallbackInstance = fallbackSupplier.apply(exception);
        final Method fallback = Methods.getFallbackMethod(fallbackInstance, method);
        try {
            return fallback.invoke(fallbackInstance, args);
        } catch (InvocationTargetException ex) {
            throw ex.getCause();
        }
    }

    private CompletableFuture<?> handleCompletionStage(CompletionStage<?> resultStage,
                                                       Method method,
                                                       Object[] args,
                                                       Predicate<Exception> filter) {
        final CompletableFuture<Object> futureResult = new CompletableFuture<>();
        resultStage.whenComplete((result, err) -> {
            if (err != null) {
                handleStageException(err, futureResult, method, args, filter);
            } else {
                futureResult.complete(result);
            }
        });
        return futureResult;
    }

    private void handleStageException(Throwable err,
                                      CompletableFuture<Object> futureResult,
                                      Method method,
                                      Object[] args,
                                      Predicate<Exception> filter) {
        if (err instanceof Exception && filter.test((Exception) err)) {
            try {
                final Object fallbackResult = executeFallback(method, (Exception) err, args);
                if (fallbackResult instanceof CompletionStage) {
                    combine((CompletionStage<?>) fallbackResult, futureResult);
                } else {
                    futureResult.complete(fallbackResult);
                }
            } catch (Throwable ex) {
                futureResult.completeExceptionally(ex);
            }
        } else {
            futureResult.completeExceptionally(err);
        }
    }

    private void combine(CompletionStage<?> fromStage, CompletableFuture<Object> toFuture) {
        fromStage.whenComplete((result, err) -> {
            if (err != null) {
                toFuture.completeExceptionally(err);
            } else {
                toFuture.complete(result);
            }
        });
    }
}
