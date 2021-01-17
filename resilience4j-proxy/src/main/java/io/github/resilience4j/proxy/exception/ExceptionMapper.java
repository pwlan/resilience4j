package io.github.resilience4j.proxy.exception;

import java.util.Optional;

/**
 * Maps exceptions from one type to another.
 */
public interface ExceptionMapper {

    /**
     * Maps the specified exception. Should return null if the exception was not mapped.
     *
     * @param exception the Exception to map.
     * @return the mapped exception or empty if the exception was not mapped.
     */
    Optional<? extends Throwable> map(Throwable exception);
}
