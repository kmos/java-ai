FROM quay.io/debezium/server:3.6.2.Final AS builder

USER root
RUN microdnf -y install gzip && \
    microdnf clean all

USER jboss
#RUN mkdir /tmp/ai && \
#   curl -kL 'https://repo1.maven.org/maven2/io/debezium/debezium-ai-embeddings-ollama/3.6.2.Final/debezium-ai-embeddings-ollama-3.6.2.Final-embeddings.tar.gz' | tar xvzf - -C /tmp/ai --strip-components=1

## Local embeddings
RUN mkdir /tmp/ai && \
   curl -kL 'https://repo1.maven.org/maven2/io/debezium/debezium-ai-embeddings-ollama/3.6.2.Final/debezium-ai-embeddings-ollama-3.6.2.Final-embeddings.tar.gz' | tar xvzf - -C /tmp/ai --strip-components=1 && \
    rm /tmp/ai/debezium-ai-embeddings-ollama-3.6.2.Final.jar
COPY debezium-ai-embeddings-openai-3.7.0-SNAPSHOT.jar /tmp/ai
COPY langchain4j-open-ai-1.0.1.jar /tmp/ai
COPY jtokkit-1.1.0.jar /tmp/ai


FROM quay.io/debezium/server:3.6.2.Final

USER root
RUN microdnf clean all

USER jboss
COPY --from=builder /tmp/ai $SERVER_HOME/lib

