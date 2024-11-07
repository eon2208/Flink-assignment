
package org.enricher;

import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.streaming.api.datastream.AsyncDataStream;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.KeyedStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.enricher.model.EnrichedMessage;
import org.enricher.model.InputMessage;
import org.enricher.model.PreEnrichmentMessage;
import org.enricher.model.TransformedMessage;
import org.enricher.operators.connector.NamedSink;
import org.enricher.operators.enricher.MessageEnricher;
import org.enricher.operators.fetcher.ServiceFetcher;
import org.enricher.operators.transformer.MessageTransformer;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

public class EnrichmentJob {

    private static final String ENRICHMENT_JOB_NAME = "Enrichment Job";

    private final StreamExecutionEnvironment env;
    private final DataStream<InputMessage> source;
    private final NamedSink<EnrichedMessage> namedSink;
    private final MessageTransformer messageTransformer;
    private final ServiceFetcher serviceFetcher;
    private final MessageEnricher messageEnricher;

    @Inject
    public EnrichmentJob(
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
                2, TimeUnit.SECONDS,
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
