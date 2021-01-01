package io.github.resilience4j.feign.test;

import io.github.resilience4j.feign.v2.FeignDecorators;
import io.github.resilience4j.feign.v2.Resilience4jFeign;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import static org.mockito.Mockito.*;

public class AsyncTestServiceExecutor extends AbstractTestServiceExecutor {

    private AsyncTestService testService;
    private AsyncTestService fallback = mock(AsyncTestService.class);

    @Override
    public void init(FeignDecorators decorators) {
        testService = Resilience4jFeign.build(decorators).target(AsyncTestService.class, MOCK_URL);
        reset(fallback);
        when(fallback.greeting()).thenReturn(CompletableFuture.completedFuture("fallback"));
    }

    @Override
    public String greeting() throws Throwable {
        try {
            return testService.greeting().get(2, TimeUnit.SECONDS);
        } catch (ExecutionException ex) {
            throw ex.getCause();
        }
    }

    @Override
    public String defaultGreeting() throws Throwable {
        try {
            return testService.defaultGreeting().get(2, TimeUnit.SECONDS);
        } catch (ExecutionException ex) {
            throw ex.getCause();
        }
    }

    @Override
    public AsyncTestService getFallback() {
        return fallback;
    }

    @Override
    public void verifyFallbackCall(int count) {
        Mockito.verify(fallback, times(count)).greeting();
    }
}
