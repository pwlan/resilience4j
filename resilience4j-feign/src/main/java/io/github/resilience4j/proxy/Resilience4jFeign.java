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

import feign.AsyncFeign;
import io.github.resilience4j.core.lang.Nullable;
import io.github.resilience4j.proxy.annotations.AnnotationDecorator;

import static java.lang.reflect.Proxy.newProxyInstance;

/**
 * TODO
 */
public final class Resilience4jFeign {

    public static <C> TargetBuilder<C> build(ProxyDecorator decorator,
                                             AsyncFeign.AsyncBuilder<C> feign) {
        return new TargetBuilder<>(decorator, feign);
    }

    public static <C> TargetBuilder<C> build(ProxyDecorator decorator) {
        return build(decorator, AsyncFeign.asyncBuilder());
    }

    public static <C> TargetBuilder<C> build() {
        return build(new AnnotationDecorator(), AsyncFeign.asyncBuilder());
    }

    public static final class TargetBuilder<C> {

        private final ProxyDecorator decorator;
        private final AsyncFeign.AsyncBuilder<C> feign;

        TargetBuilder(@Nullable ProxyDecorator decorator,
                      AsyncFeign.AsyncBuilder<C> feign) {
            this.feign = feign;
            this.decorator = decorator;
        }

        public AsyncFeign.AsyncBuilder<C> getFeign() {
            return feign;
        }

        public <T> T target(Class<T> apiType, String url) {
            final T target = feign.target(apiType, url);
            return addDecorator(target, apiType);
        }

        private <T> T addDecorator(T target, Class<T> apiType) {
            return apiType.cast(newProxyInstance(apiType.getClassLoader(),
                                                 new Class<?>[]{apiType},
                                                 new DecoratorInvocationHandler<>(apiType, target, decorator)));
        }
    }
}
