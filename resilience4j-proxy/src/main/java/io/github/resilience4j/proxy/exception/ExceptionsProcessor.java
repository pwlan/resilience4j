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
package io.github.resilience4j.proxy.exception;

import io.github.resilience4j.proxy.ProxyContext;
import io.github.resilience4j.proxy.ProxyDecorator;
import sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static io.github.resilience4j.proxy.util.AnnotationFinder.find;

/**
 * Processes {@link io.github.resilience4j.proxy.exception.Exceptions} annotations and returns
 * a corresponding {@link ProxyDecorator}.
 */
public class ExceptionsProcessor {

    private final ProxyContext context;

    public ExceptionsProcessor(ProxyContext context) {
        this.context = context;
    }

    public Optional<ProxyDecorator> process(Method method) {
        final Exceptions annotation = find(Exceptions.class, method);

        if (annotation == null) {
            return Optional.empty();
        }

        final List<ExceptionMapper> exceptionMapper = buildMapper(annotation, method);
        return Optional.of(new ExceptionsDecorator(exceptionMapper));
    }

    private List<ExceptionMapper> buildMapper(Exceptions annotation, Method method) {
        final List<ExceptionMapper> mappers = new ArrayList<>();
        for (Class<? extends ExceptionMapper> mapperClass : annotation.mappers()) {
            final ExceptionMapper mapper = context.lookup(mapperClass);
            checkCompatibility(mapper, method);
            mappers.add(mapper);
        }
        return mappers;
    }

    private void checkCompatibility(ExceptionMapper mapper, Method method) {
        try {
            final ParameterizedType optionalType = (ParameterizedType) mapper.getClass().getMethod("map", Throwable.class).getGenericReturnType();
            final Class<?> mappedType = (Class<?>) optionalType.getActualTypeArguments()[0];

            if (RuntimeException.class.isAssignableFrom(mappedType)) {
                return;
            }
            for (Class<?> exceptionType : method.getExceptionTypes()) {
                if (exceptionType.isAssignableFrom(mappedType)) {
                    return;
                }
            }
        } catch (Exception ex) {
            throw new IllegalArgumentException("The ExceptionMapper " +
                mapper.getClass() +
                " cannot be applied to " + method + "! ", ex);
        }
        throw new IllegalArgumentException("The ExceptionMapper " +
            mapper.getClass() +
            " cannot be applied to " + method +
            "! The output of the ExceptionMapper does not match any of " +
            "the declared exceptions!");
    }

}
