/*
 *
 * Copyright 2018
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
package io.github.resilience4j.proxy;

import io.github.resilience4j.proxy.annotations.AnnotationDecorator;

import static java.lang.reflect.Proxy.newProxyInstance;

/**
 * TODO
 */
public final class Resilience4jProxy {
    private final ProxyDecorator decorator;

    private Resilience4jProxy(ProxyDecorator decorator) {
        this.decorator = decorator;
    }

    public <T> T apply(Class<T> apiType, T target) {
        return apiType.cast(newProxyInstance(apiType.getClassLoader(),
                                             new Class<?>[]{apiType},
                                             new DecoratorInvocationHandler<>(apiType, target, decorator)));
    }

    public static Resilience4jProxy build(ProxyDecorator decorator) {
        return new Resilience4jProxy(decorator);
    }

    public static Resilience4jProxy build() {
        return build(new AnnotationDecorator());
    }
}
