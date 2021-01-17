package io.github.resilience4j.proxy;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
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

import static io.github.resilience4j.ratelimiter.RateLimiterConfig.custom;
import static io.github.resilience4j.ratelimiter.RateLimiterRegistry.ofDefaults;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.times;
import static org.powermock.api.mockito.PowerMockito.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({RateLimiter.class})
public class RateLimiterProxyTest {

    private Resilience4jProxy resilience4jProxy;
    private RateLimiterTestService testService;
    private RateLimiterTestService decoratedTestService;

    @Captor
    ArgumentCaptor<RateLimiter> configCaptor;

    @Before
    public void setup() {
        final RateLimiterRegistry rateLimiterRegistry = ofDefaults();
        rateLimiterRegistry.rateLimiter("rateLimiter", custom().limitForPeriod(23).build());

        final ProxyContext context = ProxyContext.builder().withRateLimiterRegistry(rateLimiterRegistry).build();
        resilience4jProxy = Resilience4jProxy.build(context);

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
        RateLimiter.decorateCheckedFunction(isA(RateLimiter.class), isA(CheckedFunction1.class));
    }

    @Test
    public void testRateLimiterDecorate() {
        decoratedTestService.rateLimited();

        verifyStatic(RateLimiter.class);
        RateLimiter.decorateCheckedFunction(configCaptor.capture(), any());
        final RateLimiter rateLimiter = configCaptor.getValue();
        assertThat(rateLimiter.getRateLimiterConfig().getLimitForPeriod()).isEqualTo(23);
    }

    @Test
    public void testAsyncRateLimiterDecorate() {
        decoratedTestService.asyncRateLimited();

        verifyStatic(RateLimiter.class);
        RateLimiter.decorateCompletionStage(configCaptor.capture(), any());
        final RateLimiter rateLimiter = configCaptor.getValue();
        assertThat(rateLimiter.getRateLimiterConfig().getLimitForPeriod()).isEqualTo(23);
    }
}

/**
 * Test Service with RateLimiter.
 */
interface RateLimiterTestService {

    @io.github.resilience4j.proxy.rateLimiter.RateLimiter(name = "rateLimiter")
    String rateLimited();

    @io.github.resilience4j.proxy.rateLimiter.RateLimiter(name = "rateLimiter")
    CompletableFuture<String> asyncRateLimited();

    String noRateLimit();
}
