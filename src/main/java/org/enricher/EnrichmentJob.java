package org.enricher;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.streaming.api.datastream.AsyncDataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.enricher.config.StreamingProperties;
import org.enricher.model.EnrichedMessage;
import org.enricher.model.InputMessage;
import org.enricher.operator.connector.kafka.sink.EnrichedMessageKafkaSink;
import org.enricher.operator.connector.kafka.source.InputMessagesKafkaSource;
import org.enricher.operator.enricher.MessageEnricher;
import org.enricher.operator.fetcher.ServiceFetcher;
import org.enricher.operator.transformation.TransformationOperator;

import java.util.concurrent.TimeUnit;

public class EnrichmentJob {

    private EnrichmentJob() {
        // no-op
    }

    public static void main(String[] args) throws Exception {
        StreamingProperties properties = new StreamingProperties(
                "localhost:9092",
                "enrichment-job",
                "input-topic",
                "output-topic",
                "localhost:8000"
        );
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        StreamingJob streamingJob = new StreamingJob(
                env,
                InputMessagesKafkaSource.createKafkaSource(properties),
                EnrichedMessageKafkaSink.createSink(properties),
                new TransformationOperator(),
                new ServiceFetcher(properties.serviceBaseUrl()),
                new MessageEnricher()
        );

        streamingJob.execute();
    }

    static class StreamingJob {
        private final StreamExecutionEnvironment env;
        private final KafkaSource<InputMessage> source;
        private final KafkaSink<EnrichedMessage> sink;
        private final TransformationOperator transformationOperator;
        private final ServiceFetcher serviceFetcher;
        private final MessageEnricher messageEnricher;

        public StreamingJob(
                StreamExecutionEnvironment env,
                KafkaSource<InputMessage> source,
                KafkaSink<EnrichedMessage> sink,
                TransformationOperator transformationOperator,
                ServiceFetcher serviceFetcher,
                MessageEnricher messageEnricher
        ) {
            this.env = env;
            this.source = source;
            this.transformationOperator = transformationOperator;
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
                    .map(transformationOperator)
                    .name("Flink Transformation");

            var fetchingStream = AsyncDataStream.orderedWait(
                    transformingStream,
                    serviceFetcher,
                    5, TimeUnit.SECONDS,
                    100
            ).name("Flink Fetcher");

            var enrichingStream = fetchingStream
                    .map(messageEnricher)
                    .name("Flink Enrichment");

            enrichingStream.sinkTo(sink)
                    .name("Flink Sink");
        }
    }
}