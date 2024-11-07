package org.enricher.operators.connector.kafka;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.formats.avro.AvroDeserializationSchema;
import org.apache.flink.formats.avro.AvroFormatOptions;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.enricher.config.KafkaProperties;
import org.enricher.model.InputMessage;

public class InputMessagesKafkaSource {

    private static final String INPUT_STREAM_NAME = "Kafka Source";
    private static final String INPUT_STREAM_UID = "b58d1169-ae14-43fb-8796-fcea4c1b6d66";

    private final StreamExecutionEnvironment env;
    private final KafkaProperties properties;

    public InputMessagesKafkaSource(
            StreamExecutionEnvironment env,
            KafkaProperties properties
    ) {
        this.env = env;
        this.properties = properties;
    }

    public SingleOutputStreamOperator<InputMessage> createKafkaSource() {
        KafkaSource<InputMessage> kafkaSource = KafkaSource.<InputMessage>builder()
                .setBootstrapServers(properties.bootstrapServers())
                .setGroupId(properties.groupId())
                .setTopics(properties.inputTopic())
                .setValueOnlyDeserializer(
                        AvroDeserializationSchema.forSpecific(
                                InputMessage.class,
                                AvroFormatOptions.AvroEncoding.JSON
                        )
                )
                .build();

        return env.fromSource(
                        kafkaSource,
                        WatermarkStrategy.noWatermarks(),
                        INPUT_STREAM_NAME
                ).name(INPUT_STREAM_NAME)
                .uid(INPUT_STREAM_UID);
    }
}
