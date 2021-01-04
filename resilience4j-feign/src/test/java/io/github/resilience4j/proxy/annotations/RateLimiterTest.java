package io.github.resilience4j.proxy.annotations;

import io.github.resilience4j.proxy.Resilience4jProxy;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.*;


public class RateLimiterTest {

    final private Resilience4jProxy resilience4jProxy = Resilience4jProxy.build();
    private RateLimiterTestService testService;
    private RateLimiterTestService decoratedTestService;

    @Before
    public void setup() {
        testService = mock(RateLimiterTestService.class);
        when(testService.rateLimited()).thenReturn("success");

        decoratedTestService = resilience4jProxy.apply(RateLimiterTestService.class, testService);
    }

    @Test
    public void rateLimited() {
        for (int i = 0; i < 10; i++) {
            decoratedTestService.rateLimited();
        }
        verify(testService, times(1)).rateLimited();
    }
}

/**
 * Test Service with fallback.
 */
interface RateLimiterTestService {

    @RateLimiter(name = "rateLimiter", limitForPeriod = 1)
    String rateLimited();
}
