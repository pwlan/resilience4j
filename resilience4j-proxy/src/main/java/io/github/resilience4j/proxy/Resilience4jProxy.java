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

import static java.lang.reflect.Proxy.newProxyInstance;

/**
 * Decorates classes based on their Resilience4jProxy annotations.
 * This is the main entrypoint of the framework.
 */
public final class Resilience4jProxy {

    private final ProxyDecorator decorator;

    private Resilience4jProxy(ProxyDecorator decorator) {
        this.decorator = decorator;
    }

    /**
     * Processes Resilience4jProxy annotations declared on an interface and returns a decorated instance.
     *
     * @param apiType an interface defining the Resilience4jProxy annotations.
     * @param target  the implementation of the interface that will be called.
     * @return the decorated instance of the <code>apiType</code> class.
     */
    public <T> T apply(Class<T> apiType, T target) {
        return apiType.cast(newProxyInstance(apiType.getClassLoader(),
            new Class<?>[]{apiType},
            new DecoratorInvocationHandler<>(target, decorator)));
    }

    private static Resilience4jProxy build(ProxyDecorator decorator) {
        return new Resilience4jProxy(decorator);
    }

    /**
     * Builds a new instance of Resilience4jProxy with am empty {@link ProxyContext}.
     */
    public static Resilience4jProxy build() {
        return build(new AnnotationDecorator(ProxyContext.builder().build()));
    }

    /**
     * Builds a new instance of Resilience4jProxy with a specified {@link ProxyContext}.
     */
    public static Resilience4jProxy build(ProxyContext context) {
        return build(new AnnotationDecorator(context));
    }
}
