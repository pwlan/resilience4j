package io.github.resilience4j.feign.v2;

import io.vavr.CheckedFunction1;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.lang.reflect.Proxy.getInvocationHandler;
import static java.lang.reflect.Proxy.isProxyClass;

class DecoratorInvocationHandler<T> implements InvocationHandler {

    private final Class<T> type;
    private final T instance;
    private final FeignDecorator invocationDecorator;
    private final Map<Method, CheckedFunction1<Object[], ?>> decoratedDispatch = new ConcurrentHashMap<>();

    DecoratorInvocationHandler(Class<T> type, T instance, FeignDecorator invocationDecorator) {
        this.type = type;
        this.instance = instance;
        this.invocationDecorator = invocationDecorator;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // switch (method.getName()) {
        //     case "equals":
        //      if (method.getParameterCount() == 1) return equals(args.length > 0 ? args[0] : null);
        //     case "hashCode":
        //      if (method.getParameterCount() == 0) return hashCode();
        //   case "toString":
        //       if (method.getParameterCount() == 0) return toString();
        // }

        return callDecoratedMethod(method, args);
    }

    private Object callDecoratedMethod(Method method, Object[] args) throws Throwable {
        return decoratedDispatch.computeIfAbsent(method, this::decorateMethod).apply(args);
    }

    private CheckedFunction1<Object[], ?> decorateMethod(Method method) {
        if (!isDeclaredByType(method)) {
            return asFunction(method);
        }
        return invocationDecorator.decorate(asFunction(method), method);
    }

    private CheckedFunction1<Object[], ?> asFunction(Method method) {
        return args -> {
            try {
                return method.invoke(instance, args);
            } catch (InvocationTargetException e) {
                throw e.getCause();
            }
        };
    }

    private boolean isDeclaredByType(Method method) {
        for (Method declaredMethod : type.getDeclaredMethods()) {
            if (declaredMethod.equals(method)) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean equals(Object obj) {
        Object compareTo = obj;
        if (compareTo == null) {
            return false;
        }
        if (isProxyClass(compareTo.getClass())) {
            compareTo = getInvocationHandler(compareTo);
        }
        if (compareTo instanceof DecoratorInvocationHandler) {
            final DecoratorInvocationHandler other = (DecoratorInvocationHandler) compareTo;
            return instance.equals(other.instance);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return instance.hashCode();
    }

    @Override
    public String toString() {
        return instance.toString();
    }
}