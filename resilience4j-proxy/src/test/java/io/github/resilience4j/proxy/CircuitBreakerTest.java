package io.github.resilience4j.proxy;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
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

import static io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom;
import static io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry.ofDefaults;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.times;
import static org.powermock.api.mockito.PowerMockito.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({CircuitBreaker.class})
public class CircuitBreakerTest {

    private Resilience4jProxy resilience4jProxy;
    private CircuitBreakerTestService testService;
    private CircuitBreakerTestService decoratedTestService;

    @Captor
    ArgumentCaptor<CircuitBreakerConfig> configCaptor;

    @Before
    public void setup() {
        final ProxyContext context = new ProxyContext();
        final CircuitBreakerRegistry circuitBreakerRegistry = ofDefaults();
        circuitBreakerRegistry.circuitBreaker("slidingWindowSize-23", custom().slidingWindowSize(23).build());
        circuitBreakerRegistry.circuitBreaker("slidingWindowSize-33", custom().slidingWindowSize(33).build());
        context.setCircuitBreakerRegistry(circuitBreakerRegistry);
        resilience4jProxy = Resilience4jProxy.build(context);

        testService = mock(CircuitBreakerTestService.class);
        when(testService.circuitBreakerMethod()).thenReturn("success");
        when(testService.asyncCircuitBreakerMethod()).thenReturn(completedFuture("success"));

        spy(CircuitBreaker.class);

        decoratedTestService = resilience4jProxy.apply(CircuitBreakerTestService.class, testService);
    }

    @Test
    public void testCircuitBreakerConfig() {
        decoratedTestService.circuitBreakerMethod();

        verifyStatic(CircuitBreaker.class, times(1));
        CircuitBreaker.decorateCheckedFunction(isA(CircuitBreaker.class), isA(CheckedFunction1.class));
    }

    @Test
    public void testAsyncCircuitBreakerConfig() {
        decoratedTestService.asyncCircuitBreakerMethod();

        verifyStatic(CircuitBreaker.class, times(1));
        CircuitBreaker.decorateCompletionStage(isA(CircuitBreaker.class), isA(Supplier.class));
    }
}

/**
 * Test Service with CircuitBreaker.
 */
interface CircuitBreakerTestService {

    @io.github.resilience4j.proxy.circuitbreaker.CircuitBreaker(name = "slidingWindowSize-23")
    String circuitBreakerMethod();

    @io.github.resilience4j.proxy.circuitbreaker.CircuitBreaker(name = "slidingWindowSize-33")
    CompletableFuture<String> asyncCircuitBreakerMethod();
}
