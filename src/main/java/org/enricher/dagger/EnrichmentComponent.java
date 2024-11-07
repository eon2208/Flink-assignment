package org.enricher.dagger;

import dagger.Component;
import org.enricher.EnrichmentJob;

import javax.inject.Singleton;

@Singleton
@Component(
        modules = {
                ContextModule.class,
                ConnectorModule.class,
                OperatorsModule.class,
                PropertiesModule.class
        })
public interface EnrichmentComponent {

    EnrichmentJob getEnrichmentJob();
}
