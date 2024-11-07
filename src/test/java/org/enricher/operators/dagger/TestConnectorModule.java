package org.enricher.operators.dagger;

import dagger.Module;
import dagger.Provides;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.enricher.model.EnrichedMessage;
import org.enricher.model.InputMessage;
import org.enricher.operators.config.EndToEndTestConfig.CollectSink;
import org.enricher.operators.connector.NamedSink;

import javax.inject.Singleton;
import java.util.Collection;
import java.util.UUID;

@Module
public class TestConnectorModule {

    private final Collection<InputMessage> inputMessages;

    public TestConnectorModule(Collection<InputMessage> inputMessages) {
        this.inputMessages = inputMessages;
    }

    @Provides
    @Singleton
    public DataStream<InputMessage> providesInputSource(
            StreamExecutionEnvironment env
    ) {
        return env.fromData(inputMessages);
    }

    @Provides
    @Singleton
    public NamedSink<EnrichedMessage> providesOutputSink() {
        return new NamedSink<>(
                "TestSink",
                UUID.randomUUID().toString(),
                new CollectSink()
        );
    }
}
