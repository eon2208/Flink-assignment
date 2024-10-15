package org.enricher.operator.fetcher;

import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.async.ResultFuture;
import org.apache.flink.streaming.api.functions.async.RichAsyncFunction;
import org.enricher.model.PreEnrichmentMessage;
import org.enricher.model.TransformedMessage;
import org.enricher.operator.fetcher.clinet.ServiceClient;
import org.enricher.operator.fetcher.clinet.impl.DefaultServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;

public class ServiceFetcher extends RichAsyncFunction<TransformedMessage, PreEnrichmentMessage> {
    private static final Logger logger = LoggerFactory.getLogger(ServiceFetcher.class);

    public static final String NAME = "Flink Fetcher";
    private final String serviceUrl;

    private transient ServiceClient serviceClient;

    public ServiceFetcher(String serviceUrl) {
        this.serviceUrl = serviceUrl;
    }

    @Override
    public void open(Configuration parameters) throws Exception {
        super.open(parameters);
        this.serviceClient = new DefaultServiceClient(serviceUrl);
    }

    @Override
    public void asyncInvoke(TransformedMessage transformedMessage, ResultFuture<PreEnrichmentMessage> resultFuture) {
        int value = transformedMessage.getValue();
        serviceClient.fetchData(value)
                .thenAccept(serviceResponse -> resultFuture.complete(
                                Collections.singleton(
                                        new PreEnrichmentMessage(transformedMessage, serviceResponse)
                                )
                        )
                )
                .exceptionally(ex -> {
                    logger.error("Error fetching data for value: {}", value, ex);
                    return null;
                });
    }

    @Override
    public void timeout(TransformedMessage input, ResultFuture<PreEnrichmentMessage> resultFuture) {
        // on timeout skip the record
        resultFuture.complete(Collections.emptyList());
    }
}
