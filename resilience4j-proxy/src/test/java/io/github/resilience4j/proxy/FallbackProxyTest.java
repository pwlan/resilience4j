package io.github.resilience4j.proxy;

import io.github.resilience4j.proxy.fallback.Fallback;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static io.github.resilience4j.proxy.test.TestHelper.failedFuture;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class FallbackProxyTest {

    final private Resilience4jProxy resilience4jProxy = Resilience4jProxy.build();
    private FallbackTestService testService;
    private FallbackTestService decoratedTestService;

    @Before
    public void setup() {
        testService = mock(FallbackTestService.class);
        when(testService.fallback()).thenThrow(new RuntimeException("test"));
        when(testService.asyncFallback()).thenReturn(failedFuture());
        decoratedTestService = resilience4jProxy.apply(FallbackTestService.class, testService);
    }

    @Test
    public void testFallback() {
        final String result = decoratedTestService.fallback();
        assertThat(result).isEqualTo("fallback");
        verify(testService, times(1)).fallback();
    }

    @Test
    public void testAsyncFallback() throws Throwable {
        final String result = decoratedTestService.asyncFallback().get(3, TimeUnit.SECONDS);
        assertThat(result).isEqualTo("fallback");
        verify(testService, times(1)).asyncFallback();
    }
}

/**
 * Test Service with fallback.
 */
@Fallback(name = "fallbackTestService", fallback = FallbackTestServiceImpl.class)
interface FallbackTestService {

    CompletableFuture<String> asyncFallback();

    String fallback();
}

/**
 * Test Service that implements fallback.
 */
class FallbackTestServiceImpl implements FallbackTestService {

    @Override
    public CompletableFuture<String> asyncFallback() {
        return completedFuture("fallback");
    }

    @Override
    public String fallback() {
        return "fallback";
    }
}