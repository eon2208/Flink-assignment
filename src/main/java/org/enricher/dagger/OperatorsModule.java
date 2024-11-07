package org.enricher.dagger;

import dagger.Module;
import dagger.Provides;
import org.enricher.config.ExternalServiceProperties;
import org.enricher.operators.enricher.MessageEnricher;
import org.enricher.operators.fetcher.ServiceFetcher;
import org.enricher.operators.transformer.MessageTransformer;

import javax.inject.Singleton;

@Module
public class OperatorsModule {

    @Provides
    @Singleton
    public MessageTransformer provideMessageTransformer() {
        return new MessageTransformer();
    }

    @Provides
    @Singleton
    public ServiceFetcher provideFetcher(ExternalServiceProperties properties) {
        return new ServiceFetcher(properties.getBaseUrl());
    }

    @Provides
    @Singleton
    public MessageEnricher provideMessageEnricher() {
        return new MessageEnricher();
    }
}
