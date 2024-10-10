package org.enricher.operator.enricher;

import org.apache.flink.api.common.functions.OpenContext;
import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.enricher.model.EnrichedMessage;
import org.enricher.model.PreEnrichmentMessage;
import org.enricher.model.ServiceResponse;

public class MessageEnricher extends RichMapFunction<PreEnrichmentMessage, EnrichedMessage> {

    private transient ValueState<ServiceResponse> serviceResponseState;

    @Override
    public void open(OpenContext openContext) throws Exception {
        super.open(openContext);
        var valueStateDescriptor = new ValueStateDescriptor<>("serviceResponseState", ServiceResponse.class);
        serviceResponseState = getRuntimeContext().getState(valueStateDescriptor);
    }

    @Override
    public EnrichedMessage map(PreEnrichmentMessage preEnrichmentMessage) throws Exception {
        var transformedMessage = preEnrichmentMessage.getTransformedMessage();
        var serviceResponse = preEnrichmentMessage.getServiceResponse();
        serviceResponseState.update(serviceResponse);

        return new EnrichedMessage(
                transformedMessage.getValue(),
                transformedMessage.getTransformed(),
                serviceResponse.getSomeIntData1(),
                serviceResponse.getSomeIntData2(),
                serviceResponse.getSomeStringData1(),
                serviceResponse.getSomeStringData2()
        );
    }
}
