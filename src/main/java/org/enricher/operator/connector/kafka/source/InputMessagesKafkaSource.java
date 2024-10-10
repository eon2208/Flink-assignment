package org.enricher.operator.connector.kafka.source;

import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.formats.avro.AvroDeserializationSchema;
import org.enricher.config.StreamingProperties;
import org.enricher.model.InputMessage;

public final class InputMessagesKafkaSource {

    private InputMessagesKafkaSource() {
        //no-op
    }

    public static KafkaSource<InputMessage> createKafkaSource(StreamingProperties properties) {
        return KafkaSource.<InputMessage>builder()
                .setBootstrapServers(properties.bootstrapServers())
                .setGroupId(properties.groupId())
                .setTopics(properties.inputTopic())
                .setValueOnlyDeserializer(AvroDeserializationSchema.forSpecific(InputMessage.class))
                .build();
    }
}
