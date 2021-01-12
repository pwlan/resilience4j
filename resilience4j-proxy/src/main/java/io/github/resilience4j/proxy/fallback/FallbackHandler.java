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
package io.github.resilience4j.proxy.fallback;

import io.github.resilience4j.core.lang.Nullable;
import io.vavr.CheckedFunction1;

import java.lang.reflect.Method;

/**
 * Used by the {@link FallbackDecorator} to handle calling fallbacks.
 * Implementations of this interface are called after every method invocation
 * and decide if a fallback is required or not.
 */
interface FallbackHandler {

    /**
     * Decides if a fallback is required and if so performs the fallback.
     *
     * @param invocationCall the function that was executed.
     *                       Implementations of the {@link FallbackHandler} may invoke this again.
     * @param method         the method that has been decorated. This should not be executed by the {@link FallbackHandler}.
     * @param args           the args passed to the method.
     * @param result         the result returned by the method.
     * @param error          the error thrown by the method.
     */
    Object handle(CheckedFunction1<Object[], ?> invocationCall,
                  Method method,
                  Object[] args,
                  @Nullable Object result,
                  @Nullable Exception error) throws Exception;

}
