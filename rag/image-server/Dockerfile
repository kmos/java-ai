FROM quay.io/debezium/server:3.6.2.Final AS builder

USER root
RUN microdnf -y install gzip && \
    microdnf clean all

USER jboss
RUN mkdir /tmp/ai && \
   curl -kL 'https://repo1.maven.org/maven2/io/debezium/debezium-ai-embeddings-ollama/3.6.2.Final/debezium-ai-embeddings-ollama-3.6.2.Final-embeddings.tar.gz' | tar xvzf - -C /tmp/ai --strip-components=1

FROM quay.io/debezium/server:3.6.2.Final

USER root
RUN microdnf clean all

USER jboss
COPY --from=builder /tmp/ai $SERVER_HOME/lib

