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

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.github.resilience4j.feign.v2.FeignDecorators;
import io.github.resilience4j.feign.test.*;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the integration of the {@link io.github.resilience4j.feign.Resilience4jFeign} with the lambda as a fallback.
 */
@RunWith(Parameterized.class)
public class Resilience4jFeignFallbackLambdaTest {

    @Parameterized.Parameters
    public static Collection<AbstractTestServiceExecutor> data() {
        return Arrays.asList(new AsyncTestServiceExecutor(),
                             new TestServiceExecutor());
    }

    @Rule
    public WireMockRule wireMockRule = new WireMockRule();

    private AbstractTestServiceExecutor testService;

    public Resilience4jFeignFallbackLambdaTest(AbstractTestServiceExecutor testService) {
        this.testService = testService;
    }

    @Before
    public void setUp() {
        final FeignDecorators decorators = FeignDecorators.builder()
                                                          .withFallback(Issue560.createLambdaFallback())
                                                          .build();
        testService.init(decorators);
    }

    @Test
    public void testFallback() throws Throwable {
        testService.givenResponse(400);

        final String result = testService.greeting();

        assertThat(result).describedAs("Result").isEqualTo("fallback");
        testService.verifyGreetingCall(1);
    }
}
