package org.enricher.operators.dagger;

import dagger.Component;
import org.enricher.EnrichmentJob;
import org.enricher.dagger.ConnectorModule;
import org.enricher.dagger.ContextModule;
import org.enricher.dagger.OperatorsModule;
import org.enricher.dagger.PropertiesModule;

import javax.inject.Singleton;

@Singleton
@Component(
        modules = {
                ContextModule.class,
                TestConnectorModule.class,
                OperatorsModule.class,
                PropertiesModule.class
        }
)
public interface TestEnrichmentComponent {

    EnrichmentJob getTestEnrichmentJob();
}
