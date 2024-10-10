package org.enricher.operator.connector.kafka.sink;

import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.flink.formats.avro.AvroSerializationSchema;
import org.enricher.config.StreamingProperties;
import org.enricher.model.EnrichedMessage;

public final class EnrichedMessageKafkaSink {

    private EnrichedMessageKafkaSink() {
        //no-op
    }

    public static KafkaSink<EnrichedMessage> createSink(StreamingProperties properties) {
        return KafkaSink.<EnrichedMessage>builder()
                .setBootstrapServers(properties.bootstrapServers())
                .setRecordSerializer(KafkaRecordSerializationSchema.builder()
                        .setTopic(properties.outputTopic())
                        .setValueSerializationSchema(AvroSerializationSchema.forSpecific(EnrichedMessage.class))
                        .build()
                ).build();
    }
}
