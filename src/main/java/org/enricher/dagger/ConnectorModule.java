package org.enricher.dagger;

import dagger.Module;
import dagger.Provides;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.enricher.config.KafkaProperties;
import org.enricher.model.EnrichedMessage;
import org.enricher.model.InputMessage;
import org.enricher.operators.connector.NamedSink;
import org.enricher.operators.connector.kafka.EnrichedMessageKafkaSink;
import org.enricher.operators.connector.kafka.InputMessagesKafkaSource;

import javax.inject.Singleton;

@Module
public class ConnectorModule {

    @Provides
    @Singleton
    public DataStream<InputMessage> providesInputSource(
            StreamExecutionEnvironment env,
            KafkaProperties properties
    ) {
        var inputMessagesKafkaSource = new InputMessagesKafkaSource(env, properties);
        return inputMessagesKafkaSource.createKafkaSource();
    }

    @Provides
    @Singleton
    public NamedSink<EnrichedMessage> providesOutputSink(
            KafkaProperties properties
    ) {
        var outputKafkaSink = new EnrichedMessageKafkaSink(properties);
        return outputKafkaSink.createSink();
    }
}
