package io.github.resilience4j.feign.v2;

import java.lang.reflect.Method;

public final class FallbackMethodFinder {

    public static Method getFallbackMethod(Object fallbackInstance, Method method) {
        final Method fallbackMethod;
        validateFallback(fallbackInstance, method);
        try {
            fallbackMethod = fallbackInstance.getClass()
                                             .getMethod(method.getName(), method.getParameterTypes());
        } catch (NoSuchMethodException | SecurityException e) {
            throw new IllegalArgumentException("Cannot use the fallback ["
                                               + fallbackInstance.getClass() + "] for ["
                                               + method.getDeclaringClass() + "]", e);
        }
        fallbackMethod.setAccessible(true);
        return fallbackMethod;
    }

    private static void validateFallback(Object fallbackInstance, Method method) {
        if (fallbackInstance.getClass().isAssignableFrom(method.getDeclaringClass())) {
            throw new IllegalArgumentException("Cannot use the fallback ["
                                               + fallbackInstance.getClass() + "] for ["
                                               + method.getDeclaringClass() + "]!");
        }
    }
}
