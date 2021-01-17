package io.github.resilience4j.proxy;

import io.github.resilience4j.proxy.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static io.github.resilience4j.proxy.test.TestHelper.callWithException;
import static io.github.resilience4j.proxy.test.TestHelper.failedFuture;
import static io.github.resilience4j.retry.RetryConfig.custom;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.powermock.api.mockito.PowerMockito.spy;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

@RunWith(PowerMockRunner.class)
@PrepareForTest({io.github.resilience4j.retry.Retry.class})
public class RetryProxyTest {

    private RetryTestService testService;
    private RetryTestService decoratedTestService;

    @Captor
    ArgumentCaptor<io.github.resilience4j.retry.Retry> configCaptor;

    @Before
    public void setup() {
        final RetryRegistry retryRegistry = RetryRegistry.ofDefaults();
        retryRegistry.retry("retry4Attempts", custom().maxAttempts(4).build());
        retryRegistry.retry("retry5Attempts", custom().maxAttempts(5).build());

        final AtomicInteger counter = new AtomicInteger(0);
        testService = mock(RetryTestService.class);
        when(testService.asyncNoRetry()).thenReturn(completedFuture("success"));
        when(testService.asyncRetryDefault()).thenReturn(failedFuture());
        when(testService.asyncRetry4Attempts()).thenReturn(failedFuture());
        when(testService.asyncRetryRecovery()).then(invocation -> {
            if (counter.incrementAndGet() < 2) {
                return failedFuture();
            }
            return completedFuture("success");
        });
        when(testService.noRetry()).thenReturn("success");
        when(testService.retryDefault()).thenThrow(new RuntimeException("test"));
        when(testService.retry4Attempts()).thenThrow(new RuntimeException("test"));
        when(testService.retryRecovery()).then(invocation -> {
            if (counter.incrementAndGet() < 2) {
                throw new RuntimeException("test");
            }
            return "success";
        });

        spy(io.github.resilience4j.retry.Retry.class);

        final ProxyContext context = ProxyContext.builder()
            .withRetryRegistry(retryRegistry)
            .build();
        decoratedTestService = Resilience4jProxy.build(context).apply(RetryTestService.class, testService);
    }

    @Test
    public void testNoRetry() {
        final String result = decoratedTestService.noRetry();
        assertThat(result).isEqualTo("success");
        verify(testService, times(1)).noRetry();
    }

    @Test
    public void testAnnotatedMethod() throws Throwable {
        callWithException(() -> decoratedTestService.retry4Attempts());
        verify(testService, times(4)).retry4Attempts();
    }

    @Test
    public void testAnnotatedClass() throws Throwable {
        callWithException(() -> decoratedTestService.retryDefault());
        verify(testService, times(3)).retryDefault();
    }

    @Test
    public void testAsyncNoRetry() throws Throwable {
        final String result = decoratedTestService.asyncNoRetry().get(3, TimeUnit.SECONDS);
        assertThat(result).isEqualTo("success");
        verify(testService, times(1)).asyncNoRetry();
    }

    @Test
    public void testRecovery() {
        final String result = decoratedTestService.retryRecovery();
        assertThat(result).isEqualTo("success");
        verify(testService, times(2)).retryRecovery();
    }

    @Test
    public void testAsyncRecovery() throws Throwable {
        final String result = decoratedTestService.asyncRetryRecovery().get(3, TimeUnit.SECONDS);
        assertThat(result).isEqualTo("success");
        verify(testService, times(2)).asyncRetryRecovery();
    }

    @Test
    public void testRetryDecorate() {
        decoratedTestService.retryRecovery();

        verifyStatic(io.github.resilience4j.retry.Retry.class);
        io.github.resilience4j.retry.Retry.decorateCheckedFunction(configCaptor.capture(), any());
        final io.github.resilience4j.retry.Retry retry = configCaptor.getValue();
        assertThat(retry.getRetryConfig().getMaxAttempts()).isEqualTo(5);
    }

    @Test
    public void testAsyncRetryDecorate() throws Exception {
        decoratedTestService.asyncRetryRecovery().get(3, TimeUnit.SECONDS);

        verifyStatic(io.github.resilience4j.retry.Retry.class);
        io.github.resilience4j.retry.Retry.decorateCompletionStage(configCaptor.capture(), any(), any());
        final io.github.resilience4j.retry.Retry retry = configCaptor.getValue();
        assertThat(retry.getRetryConfig().getMaxAttempts()).isEqualTo(5);
    }
}

/**
 * Test Data
 */
@Retry(name = "retryTestService")
interface RetryTestService {

    String noRetry();

    CompletableFuture<String> asyncNoRetry();

    String retryDefault();

    CompletableFuture<String> asyncRetryDefault();

    @Retry(name = "retry4Attempts")
    String retry4Attempts();

    @Retry(name = "retry4Attempts")
    CompletableFuture<String> asyncRetry4Attempts();

    @Retry(name = "retry5Attempts")
    String retryRecovery();

    @Retry(name = "retry5Attempts")
    CompletableFuture<String> asyncRetryRecovery();
}
