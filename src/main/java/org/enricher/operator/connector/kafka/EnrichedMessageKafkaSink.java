package org.enricher.operator.connector.kafka;

import org.apache.flink.api.connector.sink2.Sink;
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.flink.formats.avro.AvroFormatOptions;
import org.apache.flink.formats.avro.AvroSerializationSchema;
import org.enricher.config.StreamingProperties;
import org.enricher.model.EnrichedMessage;
import org.enricher.operator.connector.NamedSink;

public final class EnrichedMessageKafkaSink {

    private static final String OUTPUT_STREAM_NAME = "Kafka Output";
    private static final String OUTPUT_STREAM_UID = "69a7eeac-88db-4e04-ae37-ea1cba00c591";

    private final StreamingProperties properties;

    public EnrichedMessageKafkaSink(
            StreamingProperties properties
    ) {
        this.properties = properties;
    }

    public NamedSink<EnrichedMessage> createSink() {
        KafkaRecordSerializationSchema<EnrichedMessage> recordSerializer = KafkaRecordSerializationSchema.builder()
                .setTopic(properties.outputTopic())
                .setValueSerializationSchema(
                        AvroSerializationSchema.forSpecific(
                                EnrichedMessage.class,
                                AvroFormatOptions.AvroEncoding.JSON
                        )
                )
                .build();
        Sink<EnrichedMessage> sink = KafkaSink.<EnrichedMessage>builder()
                .setBootstrapServers(properties.bootstrapServers())
                .setRecordSerializer(recordSerializer
                ).build();

        return new NamedSink<>(OUTPUT_STREAM_NAME, OUTPUT_STREAM_UID, sink);
    }
}
