package io.github.resilience4j.feign.test;

import io.github.resilience4j.proxy.ProxyDecorators;
import io.github.resilience4j.proxy.Resilience4jFeign;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.mockito.Mockito.*;

public class TestServiceExecutor extends AbstractTestServiceExecutor {

    private TestService testService;
    private TestService fallback = mock(TestService.class);

    @Override
    public void init(ProxyDecorators decorators) {
        testService = Resilience4jFeign.build(decorators).target(TestService.class, MOCK_URL);
        reset(fallback);
        when(fallback.greeting()).thenReturn("fallback");
    }

    @Override
    public String greeting() {
        return testService.greeting();
    }

    @Override
    public String defaultGreeting() {
        return testService.defaultGreeting();
    }

    @Override
    public TestService getFallback() {
        return fallback;
    }

    @Override
    public void verifyFallbackCall(int count) {
        Mockito.verify(fallback, times(count)).greeting();
    }
}
