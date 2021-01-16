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
package io.github.resilience4j.proxy;

import io.vavr.CheckedFunction1;

import java.lang.reflect.Method;

/**
 * Decorates a method. Decorators can be stacked, allowing
 * multiple Decorators to be combined.
 */
@FunctionalInterface
public interface ProxyDecorator {

    /**
     * Decorates the invocation of a method.
     *
     * @param invocationCall represents the call to the method. This should be decorated by the
     *                       implementing class.
     * @param method         the method of the feign interface that is invoked.
     * @return the decorated invocationCall
     */
    CheckedFunction1<Object[], ?> decorate(CheckedFunction1<Object[], ?> invocationCall, Method method);

}
