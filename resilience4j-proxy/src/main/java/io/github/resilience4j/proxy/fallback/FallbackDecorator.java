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

import io.github.resilience4j.proxy.ProxyDecorator;
import io.vavr.CheckedFunction1;

import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Implementation of {@link ProxyDecorator} that decorates functions with a Fallback.
 */
public class FallbackDecorator implements ProxyDecorator {

    private final FallbackHandler fallbackHandler;

    public FallbackDecorator(FallbackHandler fallbackHandler) {
        this.fallbackHandler = fallbackHandler;
    }

    @Override
    public CheckedFunction1<Object[], ?> decorate(CheckedFunction1<Object[], ?> invocationCall, Method method) {
        return args -> {
            try {
                final Object result = invocationCall.apply(args);
                if (result instanceof CompletionStage) {
                    return handleCompletionStage((CompletionStage<?>) result, invocationCall, method, args);
                }
                return fallbackHandler.handle(invocationCall, method, args, result, null);
            } catch (Exception err) {
                return fallbackHandler.handle(invocationCall, method, args, null, err);
            }
        };
    }

    private CompletableFuture<?> handleCompletionStage(CompletionStage<?> stage,
                                                       CheckedFunction1<Object[], ?> invocationCall,
                                                       Method method,
                                                       Object[] args) {
        final CompletableFuture<Object> futureResult = new CompletableFuture<>();
        stage.whenComplete((result, err) -> handleComplete(result, err, futureResult, invocationCall, method, args));
        return futureResult;
    }

    private void handleComplete(Object result,
                                Throwable err,
                                CompletableFuture<Object> futureResult,
                                CheckedFunction1<Object[], ?> invocationCall,
                                Method method,
                                Object[] args) {
        try {
            final Object fallbackResult = fallbackHandler.handle(invocationCall, method, args, result, err);
            if (fallbackResult instanceof CompletionStage) {
                combine((CompletionStage<?>) fallbackResult, futureResult);
            } else {
                futureResult.complete(fallbackResult);
            }
        } catch (Throwable ex) {
            futureResult.completeExceptionally(ex);
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
