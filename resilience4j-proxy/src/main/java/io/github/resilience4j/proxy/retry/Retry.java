/*
 * Copyright 2019 Mahmoud Romeh
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
package io.github.resilience4j.proxy.retry;

import io.github.resilience4j.retry.RetryConfig;

import java.lang.annotation.*;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * This annotation can be applied to an interface or a method of an interface. Applying it on an interface is
 * equivalent to applying it on all its public methods. Each method can override the Retry annotation
 * specified on the interface by specifying their own annotation.
 */
@Retention(value = RetentionPolicy.RUNTIME)
@Target(value = {ElementType.METHOD, ElementType.TYPE})
@Documented
public @interface Retry {

    /**
     * @return the name of the retry.
     */
    String name();

    /**
     * @return a supplier that provides the entire config. If this is set, then all other config values are ignored.
     */
    Class<? extends Supplier<RetryConfig>> configProvider() default None.class;

    /**
     * @return the number of retries to perform.
     */
    int maxAttempts() default -1;

    /**
     * @return the duration in milliseconds to wait between retries.
     */
    long waitDuration() default -1;

    /**
     * @return the Exceptions that trigger a retry.
     */
    Class<? extends Throwable>[] retryExceptions() default {};

    /**
     * @return a predicate that decides if a retry is triggered.
     */
    Class<? extends Predicate<Throwable>> retryOnException() default None.class;

    /**
     * Indicates that there is no value specified.
     */
    abstract class None implements Predicate<Throwable>, Supplier<RetryConfig> {
    }
}

