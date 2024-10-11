ARG FLINK_VERSION=1.20.0-java17

FROM flink:${FLINK_VERSION}

# Attach shadowJar
COPY ./../build/libs/ /opt/flink/usrlib/artifacts1/