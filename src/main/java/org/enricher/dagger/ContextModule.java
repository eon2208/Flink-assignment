package org.enricher.dagger;

import dagger.Module;
import dagger.Provides;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

import javax.inject.Singleton;

@Module
public class ContextModule {

    @Provides
    @Singleton
    public StreamExecutionEnvironment providesStreamExecutionEnvironment() {
        return StreamExecutionEnvironment.getExecutionEnvironment();
    }
}