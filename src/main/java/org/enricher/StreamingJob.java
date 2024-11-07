package org.enricher;

import org.enricher.dagger.ConnectorModule;
import org.enricher.dagger.ContextModule;
import org.enricher.dagger.DaggerEnrichmentComponent;
import org.enricher.dagger.EnrichmentComponent;
import org.enricher.dagger.OperatorsModule;
import org.enricher.dagger.PropertiesModule;

public final class StreamingJob {

    private StreamingJob() {}

    public static void main(String[] args) throws Exception {

        EnrichmentComponent enrichmentComponent = DaggerEnrichmentComponent.builder()
                .connectorModule(new ConnectorModule())
                .contextModule(new ContextModule())
                .operatorsModule(new OperatorsModule())
                .propertiesModule(new PropertiesModule(args))
                .build();

        EnrichmentJob enrichmentJob = enrichmentComponent.getEnrichmentJob();

        enrichmentJob.execute();
    }
}