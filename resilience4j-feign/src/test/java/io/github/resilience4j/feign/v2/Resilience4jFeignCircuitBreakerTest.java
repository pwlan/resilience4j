/*
 *
 * Copyright 2020
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
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.feign.test.AbstractTestServiceExecutor;
import io.github.resilience4j.feign.test.AsyncTestServiceExecutor;
import io.github.resilience4j.feign.test.TestServiceExecutor;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the integration of the {@link Resilience4jFeign} with {@link CircuitBreaker}
 */
@RunWith(Parameterized.class)
public class Resilience4jFeignCircuitBreakerTest {

    @Parameterized.Parameters
    public static Collection<AbstractTestServiceExecutor> data() {
        return Arrays.asList(new AsyncTestServiceExecutor(),
                             new TestServiceExecutor());
    }

    private static final CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
                                                                                         .slidingWindowSize(3)
                                                                                         .waitDurationInOpenState(Duration.ofMillis(1000))
                                                                                         .build();
    @Rule
    public WireMockRule wireMockRule = new WireMockRule();
    private CircuitBreaker circuitBreaker;
    private AbstractTestServiceExecutor testService;

    public Resilience4jFeignCircuitBreakerTest(AbstractTestServiceExecutor testService) {
        this.testService = testService;
    }

    @Before
    public void setUp() {
        circuitBreaker = CircuitBreaker.of("test", circuitBreakerConfig);
        final FeignDecorators decorators = FeignDecorators.builder()
                                                          .withCircuitBreaker(circuitBreaker)
                                                          .build();
        testService.init(decorators);
    }

    @Test
    public void testSuccessfulCall() throws Throwable {
        final CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();

        testService.givenResponse(200);

        testService.greeting();

        testService.verifyGreetingCall(1);
        assertThat(metrics.getNumberOfSuccessfulCalls())
            .describedAs("Successful Calls")
            .isEqualTo(1);
    }

    @Test
    public void testSuccessfulCallWithDefaultMethod() throws Throwable {
        final CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();

        testService.givenResponse(200);

        testService.defaultGreeting();

        testService.verifyGreetingCall(1);
        assertThat(metrics.getNumberOfSuccessfulCalls())
            .describedAs("Successful Calls")
            .isEqualTo(1);
    }

    @Test
    public void testFailedCall() throws Throwable {
        final CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        boolean exceptionThrown = false;

        testService.givenResponse(400);

        try {
            testService.greeting();
        } catch (final FeignException ex) {
            exceptionThrown = true;
        }

        assertThat(exceptionThrown)
            .describedAs("FeignException thrown")
            .isTrue();
        assertThat(metrics.getNumberOfFailedCalls())
            .describedAs("Successful Calls")
            .isEqualTo(1);
    }

    @Test
    public void testCircuitBreakerOpen() throws Throwable {
        boolean exceptionThrown = false;
        final int threshold = circuitBreaker
                                  .getCircuitBreakerConfig()
                                  .getSlidingWindowSize() + 1;

        testService.givenResponse(400);

        for (int i = 0; i < threshold; i++) {
            try {
                testService.greeting();
            } catch (final Exception ex) {
                if(ex instanceof CallNotPermittedException) {
                    exceptionThrown = true;
                }
            }
        }

        assertThat(exceptionThrown)
            .describedAs("CallNotPermittedException thrown")
            .isTrue();
        assertThat(circuitBreaker.tryAcquirePermission())
            .describedAs("CircuitBreaker Closed")
            .isFalse();
    }


    @Test
    public void testCircuitBreakerClosed() throws Throwable {
        boolean exceptionThrown = false;
        final int threshold = circuitBreaker
                                  .getCircuitBreakerConfig()
                                  .getSlidingWindowSize() - 1;

        testService.givenResponse(400);

        for (int i = 0; i < threshold; i++) {
            try {
                testService.greeting();
            } catch (final FeignException ex) {
                // ignore
            } catch (final CallNotPermittedException ex) {
                exceptionThrown = true;
            }
        }

        assertThat(exceptionThrown)
            .describedAs("CallNotPermittedException thrown")
            .isFalse();
        assertThat(circuitBreaker.tryAcquirePermission())
            .describedAs("CircuitBreaker Closed")
            .isTrue();
    }
}
