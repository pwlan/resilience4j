package io.github.resilience4j.proxy;


import com.github.tomakehurst.wiremock.junit.WireMockRule;
import feign.FeignException;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.feign.test.AbstractTestServiceExecutor;
import io.github.resilience4j.feign.test.AsyncTestServiceExecutor;
import io.github.resilience4j.feign.test.TestServiceExecutor;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static org.mockito.Mockito.*;

/**
 * Tests the integration of the {@link io.github.resilience4j.feign.Resilience4jFeign} with a bulkhead.
 */
@RunWith(Parameterized.class)
public class Resilience4jFeignBulkheadTest {

    @Parameterized.Parameters
    public static Collection<AbstractTestServiceExecutor> data() {
        return Arrays.asList(new AsyncTestServiceExecutor(),
                             new TestServiceExecutor());
    }

    @Rule
    public WireMockRule wireMockRule = new WireMockRule();

    private Bulkhead bulkhead;
    private AbstractTestServiceExecutor testService;

    public Resilience4jFeignBulkheadTest(AbstractTestServiceExecutor testService) {
        this.testService = testService;
    }

    @Before
    public void setUp() {
        bulkhead = spy(Bulkhead.of("bulkheadTest", BulkheadConfig.ofDefaults()));
        final ProxyDecorators decorators = ProxyDecorators.builder()
                                                          .withBulkhead(bulkhead)
                                                          .build();
        testService.init(decorators);
    }

    @Test
    public void testSuccessfulCall() throws Throwable {
        testService.givenResponse(200);

        testService.greeting();

        testService.verifyGreetingCall(1);
        verify(bulkhead).acquirePermission();
    }

    @Test
    public void testSuccessfulCallWithDefaultMethod() throws Throwable {
        testService.givenResponse(200);

        testService.defaultGreeting();

        testService.verifyGreetingCall(1);
        verify(bulkhead).acquirePermission();
    }

    @Test(expected = BulkheadFullException.class)
    public void testBulkheadFull() throws Throwable {
        testService.givenResponse(200);

        when(bulkhead.tryAcquirePermission()).thenReturn(false);

        testService.greeting();

        testService.verifyGreetingCall(0);
    }

    @Test(expected = FeignException.class)
    public void testFailedCall() throws Throwable {
        testService.givenResponse(400);

        when(bulkhead.tryAcquirePermission()).thenReturn(true);

        testService.greeting();
    }
}
