package org.enricher.config;

public record StreamingProperties(
    String bootstrapServers,
    String groupId,
    String inputTopic,
    String outputTopic
){}