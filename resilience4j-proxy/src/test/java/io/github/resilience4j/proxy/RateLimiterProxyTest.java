package io.github.resilience4j.proxy;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.vavr.CheckedFunction1;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.times;
import static org.powermock.api.mockito.PowerMockito.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({RateLimiter.class})
public class RateLimiterProxyTest {

    final private Resilience4jProxy resilience4jProxy = Resilience4jProxy.build();
    private RateLimiterTestService testService;
    private RateLimiterTestService decoratedTestService;

    @Captor
    ArgumentCaptor<RateLimiterConfig> configCaptor;

    @Before
    public void setup() {
        testService = mock(RateLimiterTestService.class);
        when(testService.rateLimited()).thenReturn("success");
        when(testService.asyncRateLimited()).thenReturn(completedFuture("success"));

        spy(RateLimiter.class);

        decoratedTestService = resilience4jProxy.apply(RateLimiterTestService.class, testService);
    }

    @Test
    public void testNoRateLimit() {
        decoratedTestService.noRateLimit();

        verifyStatic(RateLimiter.class, times(0));
        RateLimiter.of(eq("rateLimiter"), any(RateLimiterConfig.class));
        RateLimiter.decorateCheckedFunction(isA(RateLimiter.class), isA(CheckedFunction1.class));
    }

    @Test
    public void testRateLimited() {
        decoratedTestService.rateLimited();

        verifyStatic(RateLimiter.class);
        RateLimiter.of(eq("rateLimiter"), any(RateLimiterConfig.class));
        RateLimiter.decorateCheckedFunction(isA(RateLimiter.class), isA(CheckedFunction1.class));
    }

    @Test
    public void testAsyncRateLimited() {
        decoratedTestService.asyncRateLimited();

        verifyStatic(RateLimiter.class);
        RateLimiter.of(eq("asyncRateLimiter"), any(RateLimiterConfig.class));
        RateLimiter.decorateCompletionStage(isA(RateLimiter.class), isA(Supplier.class));
    }

    @Test
    public void testRateLimiterConfig() {
        decoratedTestService.configuredRateLimiter();

        verifyStatic(RateLimiter.class);
        RateLimiter.of(eq("configuredRateLimiter"), configCaptor.capture());
        final RateLimiterConfig config = configCaptor.getValue();
        assertThat(config.getLimitForPeriod()).isEqualTo(23);
    }
}

/**
 * Test Service with RateLimiter.
 */
interface RateLimiterTestService {

    @io.github.resilience4j.proxy.rateLimiter.RateLimiter(name = "rateLimiter")
    String rateLimited();

    @io.github.resilience4j.proxy.rateLimiter.RateLimiter(name = "asyncRateLimiter")
    CompletableFuture<String> asyncRateLimited();

    @io.github.resilience4j.proxy.rateLimiter.RateLimiter(name = "configuredRateLimiter", limitForPeriod = 23)
    String configuredRateLimiter();

    String noRateLimit();
}
