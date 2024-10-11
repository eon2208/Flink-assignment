package org.enricher;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.streaming.api.datastream.AsyncDataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.enricher.config.StreamingProperties;
import org.enricher.model.EnrichedMessage;
import org.enricher.model.InputMessage;
import org.enricher.model.PreEnrichmentMessage;
import org.enricher.operator.connector.kafka.sink.EnrichedMessageKafkaSink;
import org.enricher.operator.connector.kafka.source.InputMessagesKafkaSource;
import org.enricher.operator.enricher.MessageEnricher;
import org.enricher.operator.fetcher.ServiceFetcher;
import org.enricher.operator.transformation.MessageTransformer;

import java.util.concurrent.TimeUnit;

import static org.enricher.config.StreamingProperties.*;

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

    static class StreamingJob {
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
            env.execute("Enrichment Job");
        }

        private void buildJobGraph() {
            var dataStreamSource = env.fromSource(source, WatermarkStrategy.noWatermarks(), "Kafka Source")
                    .keyBy(InputMessage::getValue);

            var transformingStream = dataStreamSource
                    .map(messageTransformer)
                    .name("Flink Transformer")
                    .disableChaining();

            var fetchingStream = AsyncDataStream.orderedWait(
                    transformingStream,
                    serviceFetcher,
                    5, TimeUnit.SECONDS,
                    100
            ).name("Flink Fetcher");

            var enrichingStream = fetchingStream
                    .keyBy((KeySelector<PreEnrichmentMessage, Integer>) value -> value.getTransformedMessage().getValue())
                    .map(messageEnricher)
                    .name("Flink Enrichment")
                    .disableChaining();

            enrichingStream.sinkTo(sink)
                    .name("Flink Sink");
        }
    }
}