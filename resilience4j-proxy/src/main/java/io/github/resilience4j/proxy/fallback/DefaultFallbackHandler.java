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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static io.github.resilience4j.proxy.util.Reflect.findMatchingMethod;

/**
 * An implementation of {@link FallbackHandler} that can use any Object to provide fallback methods.
 * The provided "fallback" Object is not required to implement any interface,
 * but must provide methods that match the ones for which it provides a fallback.
 * Hint: This implementation will only call the fallback when an exception is thrown.
 **/
class DefaultFallbackHandler implements FallbackHandler {

    private final Object fallback;

    DefaultFallbackHandler(Object fallback) {
        this.fallback = fallback;
    }

    @Override
    public Object handle(CheckedFunction1<Object[], ?> invocationCall,
                         Method method,
                         Object[] args,
                         @Nullable Object result,
                         @Nullable Throwable error) throws Throwable {
        if (error != null) {
            return executeFallback(method, args);
        }
        return result;
    }

    private Object executeFallback(Method method, Object[] args) throws Exception {
        try {
            final Method fallbackMethod = findMatchingMethod(fallback, method);
            return fallbackMethod.invoke(fallback, args);
        } catch (InvocationTargetException ex) {
            final Throwable ite = ex.getCause();
            if (ite instanceof Exception) {
                throw (Exception) ite;
            } else {
                throw new RuntimeException(ite);
            }
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("The fallback [" +
                fallback + "] does not define a method matching [" + method + "] ", ex);
        }
    }
}
