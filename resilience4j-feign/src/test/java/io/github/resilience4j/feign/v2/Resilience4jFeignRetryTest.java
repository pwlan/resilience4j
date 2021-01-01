/*
 *
 * Copyright 2020 Mahmoud Romeh
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
import io.github.resilience4j.feign.test.AbstractTestServiceExecutor;
import io.github.resilience4j.feign.test.AsyncTestServiceExecutor;
import io.github.resilience4j.feign.test.TestServiceExecutor;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import org.junit.runners.Parameterized.Parameters;

/**
 * Tests the integration of the {@link io.github.resilience4j.feign.Resilience4jFeign} with {@link Retry}
 */
@RunWith(Parameterized.class)
public class Resilience4jFeignRetryTest {

    @Parameters
    public static Collection<AbstractTestServiceExecutor> data() {
        return Arrays.asList(new AsyncTestServiceExecutor(),
                             new TestServiceExecutor());
    }

    @Rule
    public WireMockRule wireMockRule = new WireMockRule();

    private AbstractTestServiceExecutor testService;
    private Retry retry;

    public Resilience4jFeignRetryTest(AbstractTestServiceExecutor testService) {
        this.testService = testService;
    }

    @Before
    public void setUp() {
        retry = Retry.ofDefaults("test");
        final FeignDecorators decorators = FeignDecorators.builder()
                                                          .withDefaultScheduledExecutor()
                                                          .withRetry(retry)
                                                          .build();
        testService.init(decorators);
    }

    @Test
    public void testSuccessfulCall() throws Throwable {
        testService.givenResponse(200);

        final String result = testService.greeting();

        assertThat(result).isEqualTo("Hello, world");
        testService.verifyGreetingCall(1);
    }

    @Test
    public void testSuccessfulCallWithDefaultMethod() throws Throwable {
        testService.givenResponse(200);

        final String result = testService.defaultGreeting();

        assertThat(result).isEqualTo("Hello, world");
        testService.verifyGreetingCall(1);
    }

    @Test(expected = FeignException.class)
    public void testFailedHttpCall() throws Throwable {
        testService.givenResponse(400);
        testService.greeting();
    }

    @Test
    public void testFailedHttpCallWithRetry() throws Throwable {
        retry = Retry.of("test", RetryConfig.custom()
                                            .retryExceptions(FeignException.class)
                                            .maxAttempts(2)
                                            .build());
        final FeignDecorators decorators = FeignDecorators.builder()
                                                          .withDefaultScheduledExecutor()
                                                          .withRetry(retry)
                                                          .build();
        testService.init(decorators);
        testService.givenResponse(400);
        try {
            testService.greeting();
            throw new AssertionError("Expected Exception to be thrown!");
        } catch (Exception ex) {
            assertThat(ex).isInstanceOf(FeignException.class);
        }
        testService.verifyGreetingCall(2);
    }

    @Test
    public void testRetryOnResult() throws Throwable {
        retry = Retry.of("test", RetryConfig.<String>custom().retryOnResult(s -> s.equalsIgnoreCase("Hello, world")).maxAttempts(2).build());
        final FeignDecorators decorators = FeignDecorators.builder()
                                                          .withDefaultScheduledExecutor()
                                                          .withRetry(retry)
                                                          .build();
        testService.init(decorators);
        testService.givenResponse(200);
        testService.greeting();
        testService.verifyGreetingCall(2);
    }
}
