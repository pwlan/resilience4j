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
