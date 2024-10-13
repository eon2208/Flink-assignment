package org.enricher.config;

public record StreamingProperties(
        String bootstrapServers,
        String groupId,
        String inputTopic,
        String outputTopic,
        String serviceBaseUrl
) {
    public static String KAFKA_BOOTSTRAP_SERVERS = "kafka.bootstrap.servers";
    public static String KAFKA_GROUP_ID = "kafka.group.id";
    public static String KAFKA_INPUT_TOPIC = "kafka.input.topic";
    public static String KAFKA_OUTPUT_TOPIC = "kafka.output.topic";
    public static String SERVICE_BASE_URL = "service.base.url";
}
