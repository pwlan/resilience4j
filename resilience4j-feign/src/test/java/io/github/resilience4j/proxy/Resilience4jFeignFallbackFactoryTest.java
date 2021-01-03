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
package io.github.resilience4j.proxy;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import feign.FeignException;
import io.github.resilience4j.feign.test.*;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Function;

import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.*;

/**
 * Unit tests on fallback factories.
 */
@RunWith(Parameterized.class)
public class Resilience4jFeignFallbackFactoryTest {

    @Parameterized.Parameters
    public static Collection<AbstractTestServiceExecutor> data() {
        return Arrays.asList(new AsyncTestServiceExecutor(),
                             new TestServiceExecutor());
    }

    @Rule
    public WireMockRule wireMockRule = new WireMockRule();

    private AbstractTestServiceExecutor testService;

    public Resilience4jFeignFallbackFactoryTest(AbstractTestServiceExecutor testService) {
        this.testService = testService;
    }

    private void buildTestService(Function<Exception, ?> fallbackSupplier) {
        ProxyDecorators decorators = ProxyDecorators.builder()
                                                    .withFallbackFactory(fallbackSupplier)
                                                    .build();
        testService.init(decorators);
    }

    @Test
    public void should_successfully_get_a_response() throws Throwable {
        buildTestService(e -> "my fallback");
        testService.givenResponse(200);

        String result = testService.greeting();

        assertThat(result).isEqualTo("Hello, world");
        testService.verifyGreetingCall(1);
    }

    @Test
    public void should_lazily_fail_on_invalid_fallback() {
        buildTestService(e -> "my fallback");

        Throwable throwable = catchThrowable(testService::greeting);

        assertThat(throwable).isNotNull()
                             .hasMessageContaining(
                                 "Cannot use the fallback [class java.lang.String] for [interface io.github.resilience4j.feign.test.TestService]");
    }

    @Test
    public void should_go_to_fallback_and_consume_exception() throws Throwable {
        buildTestService(TestServiceFallbackWithException::new);
        testService.givenResponse(400);

        String result = testService.greeting();

        assertThat(result)
            .isEqualTo("Message from exception: status 400 reading TestService#greeting()");
        testService.verifyGreetingCall(1);
    }

    @Test
    public void should_go_to_fallback_and_rethrow_an_exception_thrown_in_fallback() {
        buildTestService(e -> new TestServiceFallbackThrowingException());
        testService.givenResponse(400);

        Throwable result = catchThrowable(testService::greeting);

        assertThat(result).isNotNull()
                          .isInstanceOf(RuntimeException.class)
                          .hasMessageContaining("Exception in greeting fallback");
        testService.verifyGreetingCall(1);
    }

    @Test
    public void should_go_to_fallback_and_consume_exception_with_exception_filter() throws Throwable {
        TestService uselessFallback = spy(TestService.class);
        when(uselessFallback.greeting()).thenReturn("I should not be called");
        ProxyDecorators decorators = ProxyDecorators.builder()
                                                    .withFallbackFactory(TestServiceFallbackWithException::new, FeignException.class)
                                                    .withFallbackFactory(e -> uselessFallback)
                                                    .build();
        testService.init(decorators);
        testService.givenResponse(400);

        String result = testService.greeting();

        assertThat(result)
            .isEqualTo("Message from exception: status 400 reading TestService#greeting()");
        verify(uselessFallback, times(0)).greeting();
        testService.verifyGreetingCall(1);
    }

    @Test
    public void should_go_to_second_fallback_and_consume_exception_with_exception_filter() throws Throwable {
        TestService uselessFallback = spy(TestService.class);
        when(uselessFallback.greeting()).thenReturn("I should not be called");
        ProxyDecorators decorators = ProxyDecorators.builder()
                                                    .withFallbackFactory(e -> uselessFallback, MyException.class)
                                                    .withFallbackFactory(TestServiceFallbackWithException::new)
                                                    .build();
        testService.init(decorators);
        testService.givenResponse(400);

        String result = testService.greeting();

        assertThat(result)
            .isEqualTo("Message from exception: status 400 reading TestService#greeting()");
        verify(uselessFallback, times(0)).greeting();
        testService.verifyGreetingCall(1);
    }

    @Test
    public void should_go_to_fallback_and_consume_exception_with_predicate() throws Throwable {
        TestService uselessFallback = spy(TestService.class);
        when(uselessFallback.greeting()).thenReturn("I should not be called");
        ProxyDecorators decorators = ProxyDecorators.builder()
                                                    .withFallbackFactory(TestServiceFallbackWithException::new,
                                                                         FeignException.class::isInstance)
                                                    .withFallbackFactory(e -> uselessFallback)
                                                    .build();
        testService.init(decorators);
        testService.givenResponse(400);

        String result = testService.greeting();

        assertThat(result)
            .isEqualTo("Message from exception: status 400 reading TestService#greeting()");
        verify(uselessFallback, times(0)).greeting();
        testService.verifyGreetingCall(1);
    }

    @Test
    public void should_go_to_second_fallback_and_consume_exception_with_predicate() throws Throwable {
        TestService uselessFallback = spy(TestService.class);
        when(uselessFallback.greeting()).thenReturn("I should not be called");
        ProxyDecorators decorators = ProxyDecorators.builder()
                                                    .withFallbackFactory(e -> uselessFallback, MyException.class::isInstance)
                                                    .withFallbackFactory(TestServiceFallbackWithException::new)
                                                    .build();
        testService.init(decorators);
        testService.givenResponse(400);

        String result = testService.greeting();

        assertThat(result)
            .isEqualTo("Message from exception: status 400 reading TestService#greeting()");
        verify(uselessFallback, times(0)).greeting();
        testService.verifyGreetingCall(1);
    }

    private static class MyException extends Exception {

    }
}
