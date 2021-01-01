package io.github.resilience4j.feign.test;

import feign.RequestLine;
import io.github.resilience4j.feign.FeignDecorators;
import io.github.resilience4j.feign.Resilience4jFeign;
import io.github.resilience4j.ratelimiter.RateLimiter;

import java.util.concurrent.CompletableFuture;


public interface AsyncTestService {

    @RequestLine("GET /greeting")
    CompletableFuture<String> greeting();

    default CompletableFuture<String> defaultGreeting() {
        return greeting();
    }
}
