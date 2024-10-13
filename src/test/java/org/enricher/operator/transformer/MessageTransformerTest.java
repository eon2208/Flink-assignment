package org.enricher.operator.transformer;

import org.enricher.model.InputMessage;
import org.enricher.model.TransformedMessage;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MessageTransformerTest {

    private final MessageTransformer sut = new MessageTransformer();

    @ParameterizedTest
    @ValueSource(ints = {1, 3, 5, -3, 15})
    public void shouldTransformInputMessage(int value) {
        //given
        InputMessage testInputMessage = new InputMessage(value);
        TransformedMessage expectedMessage = new TransformedMessage(
                value,
                "transformed-" + value
        );

        //when
        var result = sut.map(testInputMessage);

        //then
        assertEquals(result, expectedMessage);
    }
}
