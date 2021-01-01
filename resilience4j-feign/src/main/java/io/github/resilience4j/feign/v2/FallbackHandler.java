/*
 *
 * Copyright 2019
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 *
 *
 */
package io.github.resilience4j.feign.v2;

import io.vavr.CheckedFunction1;

import java.lang.reflect.Method;
import java.util.function.Predicate;

/**
 * Handles a fallback of of type {@param T}.
 *
 * @param <T> the type of the fallback
 */
interface FallbackHandler<T> {

    CheckedFunction1<Object[], ?> decorate(CheckedFunction1<Object[], ?> invocationCall,
                                           Method method,
                                           Predicate<Exception> filter);
}
