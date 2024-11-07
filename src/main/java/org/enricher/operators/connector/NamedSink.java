package org.enricher.operators.connector;

import org.apache.flink.api.connector.sink2.Sink;

public record NamedSink<T>(
        String sinkName,
        String sinkUid,
        Sink<T> sink
){}
