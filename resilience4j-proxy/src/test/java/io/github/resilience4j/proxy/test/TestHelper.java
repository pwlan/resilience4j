package io.github.resilience4j.proxy.test;

import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public final class TestHelper {

    public static CompletableFuture<String> failedFuture() {
        final CompletableFuture<String> result = new CompletableFuture<>();
        result.completeExceptionally(new RuntimeException("test"));
        return result;
    }

    public static void callAsyncWithException(Supplier<CompletableFuture<String>> subject) throws Throwable {
        try {
            subject.get().get(3, TimeUnit.SECONDS);
            throw new AssertionError("Expected exception to be thrown!");
        } catch (Exception ex) {
            // expected exception
        }
    }

    public static void callWithException(Supplier<String> subject) throws Throwable {
        try {
            subject.get();
            throw new AssertionError("Expected exception to be thrown!");
        } catch (Exception ex) {
            // expected exception
        }
    }
}
