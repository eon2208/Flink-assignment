package org.enricher.operator.fetcher.clinet.impl;

import org.apache.avro.io.DatumReader;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.enricher.model.ServiceResponse;
import org.enricher.operator.fetcher.clinet.ResponseFetchingException;
import org.enricher.operator.fetcher.clinet.ServiceClient;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class DefaultServiceClient implements ServiceClient {

    private static final String SERVICE_URI_FORMAT = "%s/value/%s";
    private static final HttpClient httpClient = HttpClient.newHttpClient();

    private final String serviceBaseUrl;

    public DefaultServiceClient(String serviceBaseUrl) {
        this.serviceBaseUrl = serviceBaseUrl;
    }

    @Override
    public CompletableFuture<ServiceResponse> fetchData(int value) {
        URI serviceUri = URI.create(String.format(SERVICE_URI_FORMAT, serviceBaseUrl, value));
        HttpRequest request = HttpRequest.newBuilder()
                .uri(serviceUri)
                .GET()
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    try {
                        return handleResponse(response);
                    } catch (Exception e) {
                        throw new CompletionException(e);
                    }
                });
    }

    private ServiceResponse handleResponse(HttpResponse<String> response) throws Exception {
        int statusCode = response.statusCode();
        String responseBody = response.body();

        if (statusCode >= 200 && statusCode < 300) {
            return deserializeAvroJson(responseBody);
        } else if (statusCode >= 400 && statusCode < 500) {
            throw new ResponseFetchingException(
                    String.format("Client error (status code: %d). Response: %s", statusCode, responseBody)
            );
        } else if (statusCode >= 500) {
            throw new ResponseFetchingException(
                    String.format("Server error (status code: %d). Response: %s", statusCode, responseBody)
            );
        } else {
            throw new ResponseFetchingException("Unexpected HTTP response status: " + statusCode);
        }
    }

    private ServiceResponse deserializeAvroJson(String jsonData) throws IOException {
        DatumReader<ServiceResponse> datumReader = new SpecificDatumReader<>(ServiceResponse.class);
        Decoder decoder = DecoderFactory.get().jsonDecoder(ServiceResponse.getClassSchema(), jsonData);
        return datumReader.read(null, decoder);
    }
}
