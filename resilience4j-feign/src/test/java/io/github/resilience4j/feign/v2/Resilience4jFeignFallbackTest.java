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
package io.github.resilience4j.feign.v2;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.feign.v2.FeignDecorators;
import io.github.resilience4j.feign.test.AbstractTestServiceExecutor;
import io.github.resilience4j.feign.test.AsyncTestServiceExecutor;
import io.github.resilience4j.feign.test.TestService;
import io.github.resilience4j.feign.test.TestServiceExecutor;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.*;

/**
 * Tests the integration of the {@link io.github.resilience4j.feign.Resilience4jFeign} with a fallback.
 */
@RunWith(Parameterized.class)
public class Resilience4jFeignFallbackTest {

    @Parameterized.Parameters
    public static Collection<AbstractTestServiceExecutor> data() {
        return Arrays.asList(new AsyncTestServiceExecutor(),
                             new TestServiceExecutor());
    }

    @Rule
    public WireMockRule wireMockRule = new WireMockRule();

    private AbstractTestServiceExecutor testService;

    public Resilience4jFeignFallbackTest(AbstractTestServiceExecutor testService) {
        this.testService = testService;
    }

    @Before
    public void setUp() {
        final FeignDecorators decorators = FeignDecorators.builder()
                                                          .withFallback(testService.getFallback())
                                                          .build();
        testService.init(decorators);
    }

    @Test
    public void testSuccessful() throws Throwable {
        testService.givenResponse(200);

        final String result = testService.greeting();

        assertThat(result).describedAs("Result").isNotEqualTo("Hello, world!");
        testService.verifyFallbackCall(0);
        testService.verifyGreetingCall(1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidFallback() throws Throwable {
        final FeignDecorators decorators = FeignDecorators.builder()
                                                          .withFallback("not a fallback")
                                                          .build();
        testService.init(decorators);
        testService.givenResponse(400);
        testService.greeting();
    }

    @Test
    public void testFallback() throws Throwable {
        testService.givenResponse(400);

        final String result = testService.greeting();

        assertThat(result).describedAs("Result").isNotEqualTo("Hello, world!");
        assertThat(result).describedAs("Result").isEqualTo("fallback");
        testService.verifyFallbackCall(1);
        testService.verifyGreetingCall(1);
    }


    @Test
    public void testFallbackExceptionFilter() throws Throwable {
        final TestService testServiceExceptionFallback = mock(TestService.class);
        when(testServiceExceptionFallback.greeting()).thenReturn("exception fallback");

        final FeignDecorators decorators = FeignDecorators.builder()
                                                          .withFallback(testServiceExceptionFallback, FeignException.class)
                                                          .withFallback(testService.getFallback())
                                                          .build();

        testService.init(decorators);
        testService.givenResponse(400);

        final String result = testService.greeting();

        assertThat(result).describedAs("Result").isNotEqualTo("Hello, world!");
        assertThat(result).describedAs("Result").isEqualTo("exception fallback");
        testService.verifyFallbackCall(0);
        verify(testServiceExceptionFallback, times(1)).greeting();
        testService.verifyGreetingCall(1);
    }

    @Test
    public void testFallbackExceptionFilterNotCalled() throws Throwable {
        final TestService testServiceExceptionFallback = mock(TestService.class);
        when(testServiceExceptionFallback.greeting()).thenReturn("exception fallback");

        final FeignDecorators decorators = FeignDecorators.builder()
                                                          .withFallback(testServiceExceptionFallback, CallNotPermittedException.class)
                                                          .withFallback(testService.getFallback())
                                                          .build();

        testService.init(decorators);
        testService.givenResponse(400);

        final String result = testService.greeting();

        assertThat(result).describedAs("Result").isNotEqualTo("Hello, world!");
        assertThat(result).describedAs("Result").isEqualTo("fallback");
        testService.verifyFallbackCall(1);
        verify(testServiceExceptionFallback, times(0)).greeting();
        testService.verifyGreetingCall(1);
    }

    @Test
    public void testFallbackFilter() throws Throwable {
        final TestService testServiceFilterFallback = mock(TestService.class);
        when(testServiceFilterFallback.greeting()).thenReturn("filter fallback");

        final FeignDecorators decorators = FeignDecorators.builder()
                                                          .withFallback(testServiceFilterFallback, ex -> true)
                                                          .withFallback(testService.getFallback())
                                                          .build();

        testService.init(decorators);
        testService.givenResponse(400);

        final String result = testService.greeting();

        assertThat(result).describedAs("Result").isNotEqualTo("Hello, world!");
        assertThat(result).describedAs("Result").isEqualTo("filter fallback");
        testService.verifyFallbackCall(0);
        verify(testServiceFilterFallback, times(1)).greeting();
        testService.verifyGreetingCall(1);
    }

    @Test
    public void testFallbackFilterNotCalled() throws Throwable {
        final TestService testServiceFilterFallback = mock(TestService.class);
        when(testServiceFilterFallback.greeting()).thenReturn("filter fallback");

        final FeignDecorators decorators = FeignDecorators.builder()
                                                          .withFallback(testServiceFilterFallback, ex -> false)
                                                          .withFallback(testService.getFallback())
                                                          .build();

        testService.init(decorators);
        testService.givenResponse(400);

        final String result = testService.greeting();

        assertThat(result).describedAs("Result").isNotEqualTo("Hello, world!");
        assertThat(result).describedAs("Result").isEqualTo("fallback");
        testService.verifyFallbackCall(1);
        verify(testServiceFilterFallback, times(0)).greeting();
        testService.verifyGreetingCall(1);
    }

    @Test
    public void testRevertFallback() throws Throwable {
        testService.givenResponse(400);

        testService.greeting();
        testService.givenResponse(200);
        final String result = testService.greeting();

        assertThat(result).describedAs("Result").isNotEqualTo("Hello, world!");
        testService.verifyFallbackCall(1);
        testService.verifyGreetingCall(2);
    }

}
