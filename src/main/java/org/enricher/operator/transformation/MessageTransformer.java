package org.enricher.operator.transformation;

import org.apache.flink.api.common.functions.MapFunction;
import org.enricher.model.InputMessage;
import org.enricher.model.TransformedMessage;

public class MessageTransformer implements MapFunction<InputMessage, TransformedMessage> {

    @Override
    public TransformedMessage map(InputMessage inputMessage) {
        return new TransformedMessage(
                inputMessage.getValue(),
                "transformed-" + inputMessage.getValue()
        );
    }
}
