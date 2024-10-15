package org.enricher.operator.connector;

import org.apache.flink.api.connector.sink2.Sink;

public record NamedSink<T>(
        String sinkName,
        String sinkUid,
        Sink<T> sink
){}
