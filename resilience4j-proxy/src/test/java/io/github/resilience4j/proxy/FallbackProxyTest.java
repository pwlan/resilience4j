package io.github.resilience4j.proxy;

import io.github.resilience4j.proxy.fallback.Fallback;
import org.junit.Before;
import org.junit.Test;

import java.util.UUID;
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
        when(testService.fallbackByMethodAnnotation()).thenThrow(new RuntimeException("test"));
        when(testService.fallbackProvided()).thenThrow(new RuntimeException("test"));
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
    public void testFallbackOnMethod() {
        final String result = decoratedTestService.fallbackByMethodAnnotation();
        assertThat(result).isEqualTo("fallbackByMethodAnnotation");
        verify(testService, times(1)).fallbackByMethodAnnotation();
    }

    @Test
    public void testFallbackContext() {
        final FallbackTestServiceImpl fallback = new FallbackTestServiceImpl();
        fallback.result = UUID.randomUUID().toString();
        final ProxyContext context = new ProxyContext();
        context.addFallback(FallbackTestService.class, fallback);

        decoratedTestService = Resilience4jProxy.build(context).apply(FallbackTestService.class, testService);
        final String result = decoratedTestService.fallbackProvided();

        assertThat(result).isEqualTo(fallback.result);
        verify(testService, times(1)).fallbackProvided();
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

    @Fallback(fallback = Fallback2TestServiceImpl.class)
    String fallbackByMethodAnnotation();

    @Fallback(fallback = FallbackTestService.class)
    String fallbackProvided();
}

/**
 * Test Service that implements fallback.
 */
class FallbackTestServiceImpl implements FallbackTestService {

    public String result = "fallback";

    @Override
    public CompletableFuture<String> asyncFallback() {
        return completedFuture(result);
    }

    @Override
    public String fallback() {
        return result;
    }

    @Override
    public String fallbackByMethodAnnotation() {
        return "error";
    }

    @Override
    public String fallbackProvided() {
        return result;
    }
}

class Fallback2TestServiceImpl {

    public String fallbackByMethodAnnotation() {
        return "fallbackByMethodAnnotation";
    }
}