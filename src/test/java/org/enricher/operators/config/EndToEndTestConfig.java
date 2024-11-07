package org.enricher.operators.config;

import org.apache.flink.api.connector.sink2.Sink;
import org.apache.flink.api.connector.sink2.SinkWriter;
import org.apache.flink.runtime.testutils.MiniClusterResourceConfiguration;
import org.apache.flink.test.util.MiniClusterWithClientResource;
import org.enricher.EnrichmentJob;
import org.enricher.dagger.ContextModule;
import org.enricher.dagger.OperatorsModule;
import org.enricher.dagger.PropertiesModule;
import org.enricher.model.EnrichedMessage;
import org.enricher.model.InputMessage;
import org.enricher.operators.dagger.DaggerTestEnrichmentComponent;
import org.enricher.operators.dagger.TestConnectorModule;
import org.junit.ClassRule;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public abstract class EndToEndTestConfig extends WireMockConfig {

    @BeforeAll
    static void beforeAll() throws Exception {
        flinkCluster.before();
    }

    @AfterAll
    static void afterAll() {
        flinkCluster.after();
    }

    @BeforeEach
    void beforeEach() {
        CollectSink.values.clear();
    }

    @ClassRule
    public static MiniClusterWithClientResource flinkCluster =
            new MiniClusterWithClientResource(
                    new MiniClusterResourceConfiguration.Builder()
                            .setNumberSlotsPerTaskManager(2)
                            .setNumberTaskManagers(1)
                            .build());


    protected void executeJobForInput(Collection<InputMessage> inputMessages) throws Exception {
        EnrichmentJob testEnrichmentJob = DaggerTestEnrichmentComponent.builder()
                .contextModule(new ContextModule())
                .operatorsModule(new OperatorsModule())
                .propertiesModule(new PropertiesModule("--flink.environment", "test"))
                .testConnectorModule(new TestConnectorModule(inputMessages))
                .build()
                .getTestEnrichmentJob();

        testEnrichmentJob.execute();
    }

    public static class CollectSink implements Sink<EnrichedMessage> {
        public static final List<EnrichedMessage> values = Collections.synchronizedList(new ArrayList<>());

        @Override
        public SinkWriter<EnrichedMessage> createWriter(InitContext context) {
            return new SinkWriter<>() {
                @Override
                public void write(EnrichedMessage element, Context context) {
                    values.add(element);
                }

                @Override
                public void flush(boolean endOfInput) {
                    // no-op
                }

                @Override
                public void close() {
                    // no-op
                }
            };
        }
    }
}
