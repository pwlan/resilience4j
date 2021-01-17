package io.github.resilience4j.proxy.test;

import io.vavr.CheckedFunction0;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public final class TestHelper {

    public static CompletableFuture<String> failedFuture() {
        final CompletableFuture<String> result = new CompletableFuture<>();
        result.completeExceptionally(new RuntimeException("test"));
        return result;
    }

    public static Throwable callWithException(CheckedFunction0<String> subject) {
        try {
            subject.apply();
        } catch (Throwable ex) {
            // expected exception
            return ex;
        }
        throw new AssertionError("Expected exception to be thrown!");
    }
}
