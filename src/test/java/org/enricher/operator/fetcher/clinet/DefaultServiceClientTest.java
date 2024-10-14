package org.enricher.operator.fetcher.clinet;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.enricher.model.ServiceResponse;
import org.enricher.operator.fetcher.clinet.impl.DefaultServiceClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutionException;

import static org.enricher.operator.utils.ServiceWireMockUtils.*;
import static org.junit.jupiter.api.Assertions.*;

public class DefaultServiceClientTest {

    private WireMockServer wireMockServer;

    @BeforeEach
    public void setUp() {
        wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        wireMockServer.start();
    }

    @AfterEach
    public void tearDown() {
        wireMockServer.stop();
    }

    @Test
    public void shouldReturnServiceResponse() throws ExecutionException, InterruptedException {
        //given
        ServiceClient sut = new DefaultServiceClient(wireMockServer.baseUrl());
        shouldMockTheServiceResponseForValue(wireMockServer, 1);

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
        ServiceClient sut = new DefaultServiceClient(wireMockServer.baseUrl());
        shouldMockClientErrorForValue(wireMockServer, 2);

        //when
        ExecutionException exception = assertThrows(ExecutionException.class, () -> sut.fetchData(2).get());

        //then
        assertInstanceOf(ResponseFetchingException.class, exception.getCause());
        assertTrue(exception.getCause().getMessage().contains("Client error (status code: 404)"));
    }

    @Test
    public void shouldHandleServerError() {
        //given
        ServiceClient sut = new DefaultServiceClient(wireMockServer.baseUrl());
        shouldMockServerErrorForValue(wireMockServer, 3);

        //when
        ExecutionException exception = assertThrows(ExecutionException.class, () -> sut.fetchData(3).get());

        //then
        assertInstanceOf(ResponseFetchingException.class, exception.getCause());
        assertTrue(exception.getCause().getMessage().contains("Server error (status code: 500)"));
    }

    @Test
    public void shouldHandleUnexpectedStatusCode() {
        //given
        ServiceClient sut = new DefaultServiceClient(wireMockServer.baseUrl());
        shouldMockUnexpectedStatusCodeForValue(wireMockServer, 4);

        //when
        ExecutionException exception = assertThrows(ExecutionException.class, () -> sut.fetchData(4).get());

        //then
        assertInstanceOf(ResponseFetchingException.class, exception.getCause());
        assertTrue(exception.getCause().getMessage().contains("Unexpected HTTP response status: 302"));
    }

}
