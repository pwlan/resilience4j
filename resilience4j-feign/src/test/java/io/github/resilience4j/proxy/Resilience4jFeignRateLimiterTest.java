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
package io.github.resilience4j.proxy;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import feign.FeignException;
import io.github.resilience4j.feign.test.AbstractTestServiceExecutor;
import io.github.resilience4j.feign.test.AsyncTestServiceExecutor;
import io.github.resilience4j.feign.test.TestServiceExecutor;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.*;

/**
 * Tests the integration of the {@link io.github.resilience4j.feign.Resilience4jFeign} with {@link RateLimiter}
 */
@RunWith(Parameterized.class)
public class Resilience4jFeignRateLimiterTest {

    @Parameterized.Parameters
    public static Collection<AbstractTestServiceExecutor> data() {
        return Arrays.asList(new AsyncTestServiceExecutor(),
                             new TestServiceExecutor());
    }

    @Rule
    public WireMockRule wireMockRule = new WireMockRule();

    private RateLimiter rateLimiter;
    private AbstractTestServiceExecutor testService;

    public Resilience4jFeignRateLimiterTest(AbstractTestServiceExecutor testService) {
        this.testService = testService;
    }

    @Before
    public void setUp() {
        rateLimiter = mock(RateLimiter.class);
        final ProxyDecorators decorators = ProxyDecorators.builder()
                                                          .withRateLimiter(rateLimiter)
                                                          .build();
        testService.init(decorators);
    }

    @Test
    public void testSuccessfulCall() throws Throwable {
        testService.givenResponse(200);
        when(rateLimiter.acquirePermission(1)).thenReturn(true);

        testService.greeting();

        testService.verifyGreetingCall(1);
        verify(rateLimiter).acquirePermission(anyInt());
    }

    @Test
    public void testSuccessfulCallWithDefaultMethod() throws Throwable {
        testService.givenResponse(200);
        when(rateLimiter.acquirePermission(1)).thenReturn(true);

        testService.defaultGreeting();

        testService.verifyGreetingCall(1);
        verify(rateLimiter).acquirePermission(anyInt());
    }

    @Test(expected = RequestNotPermitted.class)
    public void testRateLimiterLimiting() throws Throwable {
        testService.givenResponse(200);
        when(rateLimiter.acquirePermission(1)).thenReturn(false);
        when(rateLimiter.getRateLimiterConfig()).thenReturn(RateLimiterConfig.ofDefaults());

        testService.greeting();

        testService.verifyGreetingCall(0);
    }

    @Test(expected = FeignException.class)
    public void testFailedHttpCall() throws Throwable {
        testService.givenResponse(400);
        when(rateLimiter.acquirePermission(1)).thenReturn(true);
        testService.greeting();
    }

    @Test(expected = RequestNotPermitted.class)
    public void testRateLimiterCreateByStaticMethod() throws Throwable {
        testService.givenResponse(200);
        when(rateLimiter.acquirePermission(1)).thenReturn(false);
        when(rateLimiter.getRateLimiterConfig()).thenReturn(RateLimiterConfig.ofDefaults());

        testService.greeting();

        testService.verifyGreetingCall(0);
    }
}
