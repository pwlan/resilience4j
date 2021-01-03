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
package io.github.resilience4j.proxy.annotations;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;

import java.lang.annotation.*;
import java.time.Duration;

/**
 * This annotation can be applied to a class or a specific method. Applying it on a class is
 * equivalent to applying it on all its public methods. The annotation enables backend retry for all
 * methods where it is applied. Backend retry is performed via a retry. If using Spring,
 * {@code name} and {@code fallbackMethod} can be resolved using Spring Expression Language (SpEL).
 */
@Retention(value = RetentionPolicy.RUNTIME)
@Target(value = {ElementType.METHOD, ElementType.TYPE})
@Documented
public @interface CircuitBreaker {

    /**
     * Name of the sync retry.
     * It can be SpEL expression. If you want to use first parameter of the method as name, you can
     * express it {@code #root.args[0]}, {@code #p0} or {@code #a0}. And method name can be accessed via
     * {@code #root.methodName}
     *
     * @return the name of the sync retry.
     */
    String name();

    int slidingWindowSize() default -1;

    long waitDurationInOpenState() default -1;
}
