package io.github.resilience4j.proxy;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom;
import static io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry.ofDefaults;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.powermock.api.mockito.PowerMockito.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({CircuitBreaker.class})
public class CircuitBreakerTest {

    private CircuitBreakerTestService decoratedTestService;

    @Captor
    ArgumentCaptor<CircuitBreaker> configCaptor;

    @Before
    public void setup() {
        final CircuitBreakerRegistry circuitBreakerRegistry = ofDefaults();
        circuitBreakerRegistry.circuitBreaker("slidingWindowSize-23", custom().slidingWindowSize(23).build());
        circuitBreakerRegistry.circuitBreaker("slidingWindowSize-33", custom().slidingWindowSize(33).build());

        final CircuitBreakerTestService testService = mock(CircuitBreakerTestService.class);
        when(testService.circuitBreakerMethod()).thenReturn("success");
        when(testService.noCircuitBreakerMethod()).thenReturn("success");
        when(testService.asyncCircuitBreakerMethod()).thenReturn(completedFuture("success"));

        spy(CircuitBreaker.class);

        final ProxyContext context = ProxyContext.builder()
            .withCircuitBreakerRegistry(circuitBreakerRegistry)
            .build();
        decoratedTestService = Resilience4jProxy.build(context).apply(CircuitBreakerTestService.class, testService);
    }

    @Test
    public void testNoCircuitBreaker() {
        decoratedTestService.noCircuitBreakerMethod();

        verifyStatic(CircuitBreaker.class, times(0));
        CircuitBreaker.decorateCheckedFunction(configCaptor.capture(), any());
    }

    @Test
    public void testCircuitBreakerDecorate() {
        decoratedTestService.circuitBreakerMethod();

        verifyStatic(CircuitBreaker.class, times(1));
        CircuitBreaker.decorateCheckedFunction(configCaptor.capture(), any());
        final CircuitBreaker circuitBreaker = configCaptor.getValue();
        assertThat(circuitBreaker.getCircuitBreakerConfig().getSlidingWindowSize()).isEqualTo(23);
    }

    @Test
    public void testAsyncCircuitBreakerDecorate() throws Exception {
        decoratedTestService.asyncCircuitBreakerMethod().get(3, TimeUnit.SECONDS);

        verifyStatic(CircuitBreaker.class, times(1));
        CircuitBreaker.decorateCompletionStage(configCaptor.capture(), any());
        final CircuitBreaker circuitBreaker = configCaptor.getValue();
        assertThat(circuitBreaker.getCircuitBreakerConfig().getSlidingWindowSize()).isEqualTo(33);
    }
}

/**
 * Test Data
 */
interface CircuitBreakerTestService {

    @io.github.resilience4j.proxy.circuitbreaker.CircuitBreaker(name = "slidingWindowSize-23")
    String circuitBreakerMethod();

    @io.github.resilience4j.proxy.circuitbreaker.CircuitBreaker(name = "slidingWindowSize-33")
    CompletableFuture<String> asyncCircuitBreakerMethod();

    String noCircuitBreakerMethod();
}
