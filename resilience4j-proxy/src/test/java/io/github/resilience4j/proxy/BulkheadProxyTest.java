package io.github.resilience4j.proxy;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.vavr.CheckedFunction1;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.times;
import static org.powermock.api.mockito.PowerMockito.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Bulkhead.class})
public class BulkheadProxyTest {

    final private Resilience4jProxy resilience4jProxy = Resilience4jProxy.build();
    private BulkheadTestService testService;
    private BulkheadTestService decoratedTestService;

    @Before
    public void setup() {
        testService = mock(BulkheadTestService.class);
        when(testService.withBulkhead()).thenReturn("success");

        spy(Bulkhead.class);

        decoratedTestService = resilience4jProxy.apply(BulkheadTestService.class, testService);
    }

    @Test
    public void testWithBulkhead() {
        decoratedTestService.withBulkhead();

        verifyStatic(Bulkhead.class, times(1));
        Bulkhead.of(eq("bulkhead"), any(BulkheadConfig.class));
        Bulkhead.decorateCheckedFunction(isA(Bulkhead.class), isA(CheckedFunction1.class));
    }
}

/**
 * Test Service.
 */
interface BulkheadTestService {

    @io.github.resilience4j.proxy.bulkhead.Bulkhead(name = "bulkhead")
    String withBulkhead();
}
