package org.enricher.operator.fetcher.clinet;

import org.enricher.model.ServiceResponse;

import java.util.concurrent.CompletableFuture;

public interface ServiceClient {
    CompletableFuture<ServiceResponse> fetchData(int value);
}
