package org.enricher;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.streaming.api.datastream.AsyncDataStream;
import org.apache.flink.streaming.api.datastream.KeyedStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.enricher.config.StreamingProperties;
import org.enricher.model.EnrichedMessage;
import org.enricher.model.InputMessage;
import org.enricher.model.PreEnrichmentMessage;
import org.enricher.model.TransformedMessage;
import org.enricher.operator.connector.kafka.EnrichedMessageKafkaSink;
import org.enricher.operator.connector.kafka.InputMessagesKafkaSource;
import org.enricher.operator.enricher.MessageEnricher;
import org.enricher.operator.fetcher.ServiceFetcher;
import org.enricher.operator.transformer.MessageTransformer;

import java.util.concurrent.TimeUnit;

import static org.enricher.config.StreamingProperties.*;
import static org.enricher.operator.connector.kafka.EnrichedMessageKafkaSink.OUTPUT_STREAM_NAME;
import static org.enricher.operator.connector.kafka.EnrichedMessageKafkaSink.OUTPUT_STREAM_UID;
import static org.enricher.operator.connector.kafka.InputMessagesKafkaSource.INPUT_STREAM_NAME;
import static org.enricher.operator.connector.kafka.InputMessagesKafkaSource.INPUT_STREAM_UID;

public class EnrichmentJob {

    private EnrichmentJob() {
        // no-op
    }

    public static void main(String[] args) throws Exception {
        ParameterTool params = ParameterTool.fromArgs(args);
        StreamingProperties properties = new StreamingProperties(
                params.get(KAFKA_BOOTSTRAP_SERVERS),
                params.get(KAFKA_GROUP_ID),
                params.get(KAFKA_INPUT_TOPIC),
                params.get(KAFKA_OUTPUT_TOPIC),
                params.get(SERVICE_BASE_URL)
        );
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        StreamingJob streamingJob = new StreamingJob(
                env,
                InputMessagesKafkaSource.createKafkaSource(properties),
                EnrichedMessageKafkaSink.createSink(properties),
                new MessageTransformer(),
                new ServiceFetcher(properties.serviceBaseUrl()),
                new MessageEnricher()
        );

        streamingJob.execute();
    }

    public static class StreamingJob {

        private static final String ENRICHMENT_JOB_NAME = "Enrichment Job";

        private final StreamExecutionEnvironment env;
        private final KafkaSource<InputMessage> source;
        private final KafkaSink<EnrichedMessage> sink;
        private final MessageTransformer messageTransformer;
        private final ServiceFetcher serviceFetcher;
        private final MessageEnricher messageEnricher;

        public StreamingJob(
                StreamExecutionEnvironment env,
                KafkaSource<InputMessage> source,
                KafkaSink<EnrichedMessage> sink,
                MessageTransformer messageTransformer,
                ServiceFetcher serviceFetcher,
                MessageEnricher messageEnricher
        ) {
            this.env = env;
            this.source = source;
            this.messageTransformer = messageTransformer;
            this.serviceFetcher = serviceFetcher;
            this.messageEnricher = messageEnricher;
            this.sink = sink;
        }

        public void execute() throws Exception {
            buildJobGraph();
            env.execute(ENRICHMENT_JOB_NAME);
        }

        private void buildJobGraph() {
            KeyedStream<InputMessage, Integer> dataStreamSource = env.fromSource(
                            source,
                            WatermarkStrategy.noWatermarks(),
                            "Kafka Source"
                    ).name(INPUT_STREAM_NAME)
                    .uid(INPUT_STREAM_UID)
                    .keyBy(InputMessage::getValue);

            SingleOutputStreamOperator<TransformedMessage> transformingStream = dataStreamSource
                    .map(messageTransformer)
                    .name(MessageTransformer.NAME)
                    .disableChaining();

            SingleOutputStreamOperator<PreEnrichmentMessage> fetchingStream = AsyncDataStream.orderedWait(
                    transformingStream,
                    serviceFetcher,
                    5, TimeUnit.SECONDS,
                    100
            ).name(ServiceFetcher.NAME);

            SingleOutputStreamOperator<EnrichedMessage> enrichingStream = fetchingStream
                    .keyBy((KeySelector<PreEnrichmentMessage, Integer>) value ->
                            value.getTransformedMessage().getValue()
                    ).map(messageEnricher)
                    .name(MessageEnricher.NAME)
                    .uid(MessageEnricher.UID)
                    .disableChaining();

            enrichingStream.sinkTo(sink)
                    .name(OUTPUT_STREAM_NAME)
                    .uid(OUTPUT_STREAM_UID);
        }
    }
}