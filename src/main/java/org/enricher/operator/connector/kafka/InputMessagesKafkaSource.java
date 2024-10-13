package org.enricher.operator.connector.kafka;

import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.formats.avro.AvroDeserializationSchema;
import org.apache.flink.formats.avro.AvroFormatOptions;
import org.enricher.config.StreamingProperties;
import org.enricher.model.InputMessage;

public final class InputMessagesKafkaSource {

    public static final String INPUT_STREAM_NAME = "Kafka Source";
    public static final String INPUT_STREAM_UID = "b58d1169-ae14-43fb-8796-fcea4c1b6d66";

    private InputMessagesKafkaSource() {
        //no-op
    }

    public static KafkaSource<InputMessage> createKafkaSource(StreamingProperties properties) {
        return KafkaSource.<InputMessage>builder()
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
    }
}
