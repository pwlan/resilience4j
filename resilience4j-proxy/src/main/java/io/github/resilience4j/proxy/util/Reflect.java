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
package io.github.resilience4j.proxy.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.CompletionStage;

public final class Reflect {

    private Reflect() {
    }

    public static <T> T newInstance(Class<T> instanceClass) throws NoSuchMethodException,
                                                                   IllegalAccessException,
                                                                   InvocationTargetException,
                                                                   InstantiationException {
        final Constructor<T> constructor = instanceClass.getDeclaredConstructor();
        constructor.setAccessible(true);
        return constructor.newInstance();
    }

    public static <T> T newInstance(Class<T> instanceClass, Map<Class<?>, Object> cache) {
        try {
            final Object result = cache.computeIfAbsent(instanceClass, key -> {
                try {
                    return newInstance(instanceClass);
                } catch (Exception e) {
                    throw new IllegalArgumentException("TODO");
                }
            });
            return (T) result;
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("TODO");
        }
    }

    public static Method findMatchingMethod(Object instance, Method method) {
        final Method fallbackMethod;
        try {
            fallbackMethod = instance.getClass().getMethod(method.getName(), method.getParameterTypes());
            if (fallbackMethod.getReturnType() != method.getReturnType()) {
                throw new IllegalArgumentException("Cannot use the fallback ["
                                                   + instance.getClass() + "] for ["
                                                   + method.getDeclaringClass() + "]");
            }
        } catch (NoSuchMethodException | SecurityException e) {
            throw new IllegalArgumentException("Cannot use the fallback ["
                                               + instance.getClass() + "] for ["
                                               + method.getDeclaringClass() + "]", e);
        }
        fallbackMethod.setAccessible(true);
        return fallbackMethod;
    }

    private static void validateFallback(Object fallbackInstance, Method method) {
        // TODO this is broken
        if (fallbackInstance.getClass().isAssignableFrom(method.getDeclaringClass())) {
            throw new IllegalArgumentException("Cannot use the fallback ["
                                               + fallbackInstance.getClass() + "] for ["
                                               + method.getDeclaringClass() + "]!");
        }
    }

    public static boolean isAsync(Method method) {
        return CompletionStage.class.isAssignableFrom(method.getReturnType());
    }
}
