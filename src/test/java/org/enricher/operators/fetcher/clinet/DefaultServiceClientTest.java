package org.enricher.operators.fetcher.clinet;

import org.enricher.model.ServiceResponse;
import org.enricher.operators.config.WireMockConfig;
import org.enricher.operators.fetcher.clinet.impl.DefaultServiceClient;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DefaultServiceClientTest extends WireMockConfig {

    private final ServiceClient sut = new DefaultServiceClient(getBaseUrl());

    @Test
    public void shouldReturnServiceResponse() throws ExecutionException, InterruptedException {
        //given
        shouldMockTheServiceResponseForValue(1);

        //when
        ServiceResponse response = sut.fetchData(1).get();

        //then
        assertEquals(response.getSomeIntData1(), 1);
        assertEquals(response.getSomeIntData2(), 1);
        assertEquals(response.getSomeStringData1(), "1");
        assertEquals(response.getSomeStringData2(), "1");
    }

    @Test
    public void shouldHandleClientError() {
        //given
        shouldMockClientErrorForValue(2);

        //when
        ExecutionException exception = assertThrows(ExecutionException.class, () -> sut.fetchData(2).get());

        //then
        assertInstanceOf(ResponseFetchingException.class, exception.getCause());
        assertTrue(exception.getCause().getMessage().contains("Client error (status code: 404)"));
    }

    @Test
    public void shouldHandleServerError() {
        //given
        shouldMockServerErrorForValue(3);

        //when
        ExecutionException exception = assertThrows(ExecutionException.class, () -> sut.fetchData(3).get());

        //then
        assertInstanceOf(ResponseFetchingException.class, exception.getCause());
        assertTrue(exception.getCause().getMessage().contains("Server error (status code: 500)"));
    }

    @Test
    public void shouldHandleUnexpectedStatusCode() {
        //given
        shouldMockUnexpectedStatusCodeForValue(4);

        //when
        ExecutionException exception = assertThrows(ExecutionException.class, () -> sut.fetchData(4).get());

        //then
        assertInstanceOf(ResponseFetchingException.class, exception.getCause());
        assertTrue(exception.getCause().getMessage().contains("Unexpected HTTP response status: 302"));
    }

}
