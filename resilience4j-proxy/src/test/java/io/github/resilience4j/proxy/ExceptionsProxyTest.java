package io.github.resilience4j.proxy;

import io.github.resilience4j.proxy.exception.ExceptionMapper;
import io.github.resilience4j.proxy.exception.Exceptions;
import org.junit.Before;
import org.junit.Test;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static io.github.resilience4j.proxy.test.TestHelper.callWithException;
import static io.github.resilience4j.proxy.test.TestHelper.failedFuture;
import static java.util.Optional.of;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ExceptionsProxyTest {

    private ExceptionsTestService decoratedTestService;

    @Before
    public void setup() throws Exception {
        final ExceptionsTestService testService = mock(ExceptionsTestService.class);
        when(testService.callWithNoExceptions()).thenReturn("success");
        when(testService.callWithNoExceptionMapping()).thenThrow(new ExceptionsTestService.ServiceException());
        when(testService.callWithRuntimeException()).thenThrow(new RuntimeException("test"));
        when(testService.callWithException()).thenThrow(new ExceptionsTestService.ServiceException());
        when(testService.callWithInvalidExceptionMapping()).thenThrow(new ExceptionsTestService.ServiceException());
        when(testService.asyncCallWithRuntimeException()).thenReturn(failedFuture());

        final ProxyContext context = ProxyContext.builder()
            .withExceptionMapper(ExtendedServiceException.class, new ExtendedServiceException())
            .build();
        decoratedTestService = Resilience4jProxy.build(context).apply(ExceptionsTestService.class, testService);
    }

    @Test
    public void testNoExceptionMapping() {
        final Throwable err = callWithException(() -> decoratedTestService.callWithNoExceptionMapping());
        assertThat(err).isInstanceOf(ExceptionsTestService.ServiceException.class);
    }

    @Test
    public void testSuccessCall() {
        final String result = decoratedTestService.callWithNoExceptions();
        assertThat(result).isEqualTo("success");
    }

    @Test
    public void testRuntimeExceptionMapping() {
        final Throwable err = callWithException(() -> decoratedTestService.callWithRuntimeException());
        assertThat(err).isInstanceOf(MappedRuntimeException.class);
    }

    @Test
    public void testAsyncRuntimeExceptionMapping() {
        final Throwable err = callWithException(() -> decoratedTestService.asyncCallWithRuntimeException().get(3, SECONDS));
        assertThat(err.getCause()).isInstanceOf(MappedRuntimeException.class);
    }

    @Test
    public void testExceptionMapping() {
        final Throwable err = callWithException(() -> decoratedTestService.callWithException());
        assertThat(err).isInstanceOf(ExtendedServiceException.class);
    }

    @Test
    public void testExceptionMappingValidationFails() {
        final Throwable err = callWithException(() -> decoratedTestService.callWithInvalidExceptionMapping());
        assertThat(err).isInstanceOf(IllegalArgumentException.class);
    }
}

/**
 * Test Data
 */
interface ExceptionsTestService {

    @Exceptions(mappers = {MappedRuntimeException.class})
    String callWithRuntimeException();

    @Exceptions(mappers = MappedRuntimeException.class)
    CompletableFuture<String> asyncCallWithRuntimeException();

    @Exceptions(mappers = ExtendedServiceException.class)
    String callWithException() throws ServiceException;

    @Exceptions(mappers = MappedException.class)
    String callWithInvalidExceptionMapping() throws ServiceException;

    String callWithNoExceptionMapping() throws ServiceException;

    @Exceptions(mappers = MappedRuntimeException.class)
    String callWithNoExceptions();

    class ServiceException extends Exception {
    }
}

class MappedRuntimeException extends RuntimeException implements ExceptionMapper {

    @Override
    public Optional<MappedRuntimeException> map(Throwable exception) {
        return of(this);
    }
}

class MappedException extends Exception implements ExceptionMapper {

    @Override
    public Optional<MappedException> map(Throwable exception) {
        return of(this);
    }
}

class ExtendedServiceException extends ExceptionsTestService.ServiceException implements ExceptionMapper {

    @Override
    public Optional<ExtendedServiceException> map(Throwable exception) {
        return of(this);
    }
}