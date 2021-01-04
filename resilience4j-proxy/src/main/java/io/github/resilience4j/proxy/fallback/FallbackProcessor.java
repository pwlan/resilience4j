package io.github.resilience4j.proxy.fallback;

import io.github.resilience4j.proxy.reflect.Methods;

import java.lang.reflect.Method;
import java.util.Optional;

import static io.github.resilience4j.proxy.reflect.AnnotationFinder.find;
import static io.github.resilience4j.proxy.reflect.Methods.newInstance;

public class FallbackProcessor {

    public Optional<Object> process(Method method) {
        final Fallback annotation = find(Fallback.class, method);

        if (annotation == null) {
            return Optional.empty();
        }

        try {
            return Optional.of(newInstance(annotation.fallback()));
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid Fallback!", e);
        }
    }
}
