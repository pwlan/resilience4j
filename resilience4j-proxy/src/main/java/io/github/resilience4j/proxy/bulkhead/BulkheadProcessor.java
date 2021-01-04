package io.github.resilience4j.proxy.bulkhead;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.core.lang.Nullable;

import java.lang.reflect.Method;
import java.util.Optional;

import static io.github.resilience4j.proxy.reflect.AnnotationFinder.find;

public class BulkheadProcessor {

    public Optional<Bulkhead> process(Method method) {
        final io.github.resilience4j.proxy.bulkhead.Bulkhead annotation =
            find(io.github.resilience4j.proxy.bulkhead.Bulkhead.class, method);

        if (annotation == null) {
            return Optional.empty();
        }

        final BulkheadConfig.Builder config = BulkheadConfig.custom();

        // TODO add config

        return Optional.of(Bulkhead.of(annotation.name(), config.build()));
    }
}
