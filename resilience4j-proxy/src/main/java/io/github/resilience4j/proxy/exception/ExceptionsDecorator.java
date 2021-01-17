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
package io.github.resilience4j.proxy.exception;

import io.github.resilience4j.proxy.ProxyDecorator;
import io.vavr.CheckedFunction1;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Implementation of {@link ProxyDecorator} that decorates functions with an {@link ExceptionMapper}.
 */
class ExceptionsDecorator implements ProxyDecorator {

    private final List<ExceptionMapper> exceptionMappers;

    ExceptionsDecorator(List<ExceptionMapper> exceptionMappers) {
        this.exceptionMappers = exceptionMappers;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CheckedFunction1<Object[], ?> decorate(CheckedFunction1<Object[], ?> invocationCall, Method method) {
        return args -> {
            try {
                final Object result = invocationCall.apply(args);
                if (result instanceof CompletionStage) {
                    return handleCompletionStage((CompletionStage<?>) result);
                }
                return result;
            } catch (Exception err) {
                throw map(err);
            }
        };
    }

    private CompletableFuture<?> handleCompletionStage(CompletionStage<?> stage) {
        final CompletableFuture<Object> futureResult = new CompletableFuture<>();
        stage.whenComplete((result, err) -> {
            if (err != null) {
                futureResult.completeExceptionally(map(err));
            } else {
                futureResult.complete(result);
            }
        });
        return futureResult;
    }

    private Throwable map(Throwable err) {
        for (ExceptionMapper exceptionMapper : exceptionMappers) {
            final Optional<? extends Throwable> mappedErr = exceptionMapper.map(err);
            if (mappedErr.isPresent()) {
                return mappedErr.get();
            }
        }
        return err;
    }
}
