package io.github.resilience4j.proxy.util;

import io.github.resilience4j.proxy.fallback.Fallback;
import io.github.resilience4j.proxy.retry.Retry;
import org.junit.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

public final class AnnotationFinderTest {

    @Test
    public void testFindAnnotationOnMethod() throws Throwable {
        final Method method = AnnotationFinderService.class.getDeclaredMethod("greeting");
        final Retry retry = AnnotationFinder.find(Retry.class, method);
        assertThat(retry).isNotNull();
        assertThat(retry.name()).isEqualTo("greeting");
    }

    @Test
    public void testFindAnnotationOnClass() throws Throwable {
        final Method method = AnnotationFinderService.class.getDeclaredMethod("greeting");
        final Fallback retry = AnnotationFinder.find(Fallback.class, method);
        assertThat(retry).isNotNull();
    }
}

@Fallback(name = "test", fallback = AnnotationFinderTest.class)
interface AnnotationFinderService {

    @Retry(name = "greeting")
    String greeting();

}
