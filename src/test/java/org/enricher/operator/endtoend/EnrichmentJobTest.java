package org.enricher.operator.endtoend;

import org.enricher.model.EnrichedMessage;
import org.enricher.model.InputMessage;
import org.enricher.operator.config.EndToEndTestConfig;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class EnrichmentJobTest extends EndToEndTestConfig {

    @Test
    void shouldEnrichInputMessages() throws Exception {
        //given
        List<InputMessage> inputMessages = List.of(new InputMessage(1), new InputMessage(2), new InputMessage(3));
        List<EnrichedMessage> expectedOutput = List.of(
                createExpectedOutputMessageForValue(1),
                createExpectedOutputMessageForValue(2),
                createExpectedOutputMessageForValue(3)
        );
        shouldMockTheServiceResponseForValues(1, 2, 3);

        //when
        executeJobForInput(inputMessages);

        //then
        assertEquals(CollectSink.values.size(), 3);
        assertArrayEquals(expectedOutput.toArray(), CollectSink.values.toArray());
    }

    @Test
    void shouldSkipTheMessageIfServiceFails() throws Exception {
        //given
        List<InputMessage> inputMessages = List.of(new InputMessage(1), new InputMessage(2));
        List<EnrichedMessage> expectedOutput = Collections.singletonList(
                createExpectedOutputMessageForValue(2)
        );
        shouldMockClientErrorForValue(1);
        shouldMockTheServiceResponseForValues(2);

        //when
        executeJobForInput(inputMessages);

        //then
        assertEquals(CollectSink.values.size(), 1);
        assertArrayEquals(expectedOutput.toArray(), CollectSink.values.toArray());
    }

    private EnrichedMessage createExpectedOutputMessageForValue(int value) {
        return new EnrichedMessage(
                value,
                "transformed-" + value,
                value,
                value,
                String.valueOf(value),
                String.valueOf(value)
        );
    }


}
