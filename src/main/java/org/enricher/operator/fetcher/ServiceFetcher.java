package org.enricher.operator.fetcher;

import org.apache.avro.io.DatumReader;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.flink.streaming.api.functions.async.AsyncFunction;
import org.apache.flink.streaming.api.functions.async.ResultFuture;
import org.enricher.model.PreEnrichmentMessage;
import org.enricher.model.ServiceResponse;
import org.enricher.model.TransformedMessage;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;

public class ServiceFetcher implements AsyncFunction<TransformedMessage, PreEnrichmentMessage> {
    private static final String SERVICE_URI_FORMAT = "%s/value/%s";

    private final String serviceBaseUrl;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    public ServiceFetcher(String serviceUrl) {
        this.serviceBaseUrl = serviceUrl;
    }

    @Override
    public void asyncInvoke(TransformedMessage transformedMessage, ResultFuture<PreEnrichmentMessage> resultFuture) {
        var serviceUri = URI.create(String.format(SERVICE_URI_FORMAT, serviceBaseUrl, transformedMessage.getValue()));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(serviceUri)
                .GET()
                .build();

        CompletableFuture<HttpResponse<String>> responseFuture = httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString());

        responseFuture.thenAccept(response -> {
            try {
                handleResponse(response, transformedMessage, resultFuture);
            } catch (Exception ex) {
                resultFuture.completeExceptionally(ex);
            }
        }).exceptionally(ex -> {
            resultFuture.completeExceptionally(new RuntimeException("HTTP request failed", ex));
            return null;
        });
    }

    private void handleResponse(HttpResponse<String> response, TransformedMessage transformedMessage, ResultFuture<PreEnrichmentMessage> resultFuture) throws Exception {
        int statusCode = response.statusCode();
        String responseBody = response.body();

        if (statusCode >= 200 && statusCode < 300) {
            handleSuccess(transformedMessage, resultFuture, responseBody);
        } else if (statusCode >= 400 && statusCode < 500) {
            handleError("Client error (status code: %d). Response: %s", statusCode, responseBody, resultFuture);
        } else if (statusCode >= 500) {
            handleError("Server error (status code: %d). Response: %s", statusCode, responseBody, resultFuture);
        } else {
            resultFuture.completeExceptionally(new ResponseFetchingException("Unexpected HTTP response status: " + statusCode));
        }
    }

    private static void handleError(String format, int statusCode, String responseBody, ResultFuture<PreEnrichmentMessage> resultFuture) {
        String errorMessage = String.format(format, statusCode, responseBody);
        resultFuture.completeExceptionally(new ResponseFetchingException(errorMessage));
    }

    private void handleSuccess(TransformedMessage transformedMessage, ResultFuture<PreEnrichmentMessage> resultFuture, String responseBody) throws IOException {
        ServiceResponse serviceResponse = deserializeAvroJson(responseBody);
        PreEnrichmentMessage preEnrichmentMessage = new PreEnrichmentMessage(transformedMessage, serviceResponse);
        resultFuture.complete(Collections.singleton(preEnrichmentMessage));
    }

    public ServiceResponse deserializeAvroJson(String jsonData) throws IOException {
        DatumReader<ServiceResponse> datumReader = new SpecificDatumReader<>(ServiceResponse.class);
        Decoder decoder = DecoderFactory.get().jsonDecoder(ServiceResponse.getClassSchema(), jsonData);
        return datumReader.read(null, decoder);
    }
}
