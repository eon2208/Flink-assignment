package org.enricher.operators.fetcher.clinet;

import org.enricher.model.ServiceResponse;

import java.util.concurrent.CompletableFuture;

public interface ServiceClient {
    CompletableFuture<ServiceResponse> fetchData(int value);
}
