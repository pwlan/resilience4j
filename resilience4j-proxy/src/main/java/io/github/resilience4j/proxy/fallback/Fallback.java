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

import java.lang.annotation.*;

/**
 * Adds the Fallback mechanism.
 * This annotation can be applied to an interface or a method of an interface. Applying it on an interface is
 * equivalent to applying it on all its methods. Each annotation on a method overrides any class level annotations.
 */
@Retention(value = RetentionPolicy.RUNTIME)
@Target(value = {ElementType.METHOD, ElementType.TYPE})
@Documented
public @interface Fallback {

    /**
     * @return the name of the fallback.
     */
    String name();

    /**
     * @return the fallback. The fallback is either an implementation of {@link FallbackHandler}
     * or a class that provides methods with thew exact signature of the method for which it provides a fallback.
     */
    Class<?> fallback();
}
