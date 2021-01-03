package io.github.resilience4j.feign.test;

import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import io.github.resilience4j.proxy.ProxyDecorators;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

public abstract class AbstractTestServiceExecutor {

    protected static final String MOCK_URL = "http://localhost:8080/";
    private static final RequestPatternBuilder PATH_GREETING = getRequestedFor(urlPathEqualTo("/greeting"));

    public abstract void init(ProxyDecorators decorators);

    public abstract String greeting() throws Throwable;

    public abstract String defaultGreeting() throws Throwable;

    public abstract Object getFallback();

    public void verifyGreetingCall(int count) {
        verify(count, PATH_GREETING);
    }

    public abstract void verifyFallbackCall(int count);

    public void givenResponse(int responseCode) {
        stubFor(get(urlPathEqualTo("/greeting"))
                    .willReturn(aResponse()
                                    .withStatus(responseCode)
                                    .withHeader("Content-Type", "text/plain")
                                    .withBody("Hello, world")));
    }
}
