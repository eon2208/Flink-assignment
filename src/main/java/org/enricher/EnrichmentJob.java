package org.enricher;

import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.AsyncDataStream;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.KeyedStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.enricher.config.StreamingProperties;
import org.enricher.model.EnrichedMessage;
import org.enricher.model.InputMessage;
import org.enricher.model.PreEnrichmentMessage;
import org.enricher.model.TransformedMessage;
import org.enricher.operator.connector.NamedSink;
import org.enricher.operator.connector.kafka.EnrichedMessageKafkaSink;
import org.enricher.operator.connector.kafka.InputMessagesKafkaSource;
import org.enricher.operator.enricher.MessageEnricher;
import org.enricher.operator.fetcher.ServiceFetcher;
import org.enricher.operator.transformer.MessageTransformer;

import java.util.concurrent.TimeUnit;

import static org.enricher.config.StreamingProperties.KAFKA_BOOTSTRAP_SERVERS;
import static org.enricher.config.StreamingProperties.KAFKA_GROUP_ID;
import static org.enricher.config.StreamingProperties.KAFKA_INPUT_TOPIC;
import static org.enricher.config.StreamingProperties.KAFKA_OUTPUT_TOPIC;
import static org.enricher.config.StreamingProperties.SERVICE_BASE_URL;

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
        StreamingJob streamingJob = prepareStreamingJob(properties);

        streamingJob.execute();
    }

    private static StreamingJob prepareStreamingJob(StreamingProperties properties) {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        InputMessagesKafkaSource inputSource = new InputMessagesKafkaSource(env, properties);
        EnrichedMessageKafkaSink sink = new EnrichedMessageKafkaSink(properties);
        return new StreamingJob(
                env,
                inputSource.createKafkaSource(),
                sink.createSink(),
                new MessageTransformer(),
                new ServiceFetcher(properties.serviceBaseUrl()),
                new MessageEnricher()
        );
    }

    public static class StreamingJob {

        private static final String ENRICHMENT_JOB_NAME = "Enrichment Job";

        private final StreamExecutionEnvironment env;
        private final DataStream<InputMessage> source;
        private final NamedSink<EnrichedMessage> namedSink;
        private final MessageTransformer messageTransformer;
        private final ServiceFetcher serviceFetcher;
        private final MessageEnricher messageEnricher;

        public StreamingJob(
                StreamExecutionEnvironment env,
                DataStream<InputMessage> source,
                NamedSink<EnrichedMessage> namedSink,
                MessageTransformer messageTransformer,
                ServiceFetcher serviceFetcher,
                MessageEnricher messageEnricher
        ) {
            this.env = env;
            this.source = source;
            this.messageTransformer = messageTransformer;
            this.serviceFetcher = serviceFetcher;
            this.messageEnricher = messageEnricher;
            this.namedSink = namedSink;
        }

        public void execute() throws Exception {
            buildJobGraph();
            env.execute(ENRICHMENT_JOB_NAME);
        }

        private void buildJobGraph() {
            KeyedStream<InputMessage, Integer> dataStreamSource = source
                    .keyBy(InputMessage::getValue);

            SingleOutputStreamOperator<TransformedMessage> transformingStream = dataStreamSource
                    .map(messageTransformer)
                    .name(MessageTransformer.NAME);

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
                    .uid(MessageEnricher.UID);

            enrichingStream.sinkTo(namedSink.sink())
                    .name(namedSink.sinkName())
                    .uid(namedSink.sinkUid());
        }
    }
}