package org.enricher.operators.enricher;

import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.streaming.api.operators.StreamMap;
import org.apache.flink.streaming.runtime.streamrecord.StreamRecord;
import org.apache.flink.streaming.util.KeyedOneInputStreamOperatorTestHarness;
import org.enricher.model.EnrichedMessage;
import org.enricher.model.PreEnrichmentMessage;
import org.enricher.model.ServiceResponse;
import org.enricher.model.TransformedMessage;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MessageEnricherTest {

    private KeyedOneInputStreamOperatorTestHarness<Integer, PreEnrichmentMessage, EnrichedMessage> testHarness;

    @Before
    public void setUp() throws Exception {
        testHarness = new KeyedOneInputStreamOperatorTestHarness<>(
                new StreamMap<>(new MessageEnricher()),
                (KeySelector<PreEnrichmentMessage, Integer>) value -> value.getTransformedMessage().getValue(),
                TypeInformation.of(Integer.class)
        );
        testHarness.open();
    }

    @Test
    public void shouldEnrichInputMessageAndSaveLastServiceResponseInState() throws Exception {
        //given
        TransformedMessage transformedMessage = new TransformedMessage(1, "transformed-1");
        ServiceResponse serviceResponse = new ServiceResponse(1, 1, "1", "1");
        PreEnrichmentMessage message = new PreEnrichmentMessage(transformedMessage, serviceResponse);

        //when
        testHarness.processElement(new StreamRecord<>(message));

        //then
        List<EnrichedMessage> operatorOutputValues = testHarness.extractOutputValues();
        assertEquals(operatorOutputValues.size(), 1);

        EnrichedMessage enrichedMessage = operatorOutputValues.get(0);
        assertEquals(enrichedMessage.getValue(), 1);
        assertEquals(enrichedMessage.getTransformed(), "transformed-1");
        assertEquals(enrichedMessage.getSomeIntData1(), 1);
        assertEquals(enrichedMessage.getSomeIntData2(), 1);
        assertEquals(enrichedMessage.getSomeStringData1(), "1");
        assertEquals(enrichedMessage.getSomeStringData2(), "1");

        assertEquals(testHarness.numKeyedStateEntries(), 1);
    }
}
