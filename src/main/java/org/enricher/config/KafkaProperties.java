package org.enricher.config;

public record KafkaProperties(
        String bootstrapServers,
        String groupId,
        String inputTopic,
        String outputTopic
) {
}
