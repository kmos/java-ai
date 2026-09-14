package io.mosfet.rag.cli;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.RegisterAiService;

@RegisterAiService(retrievalAugmentor = MilvusRetrievalAugmentor.class)
public interface Chat {

    @SystemMessage("""
            You are a helpful assistant. Answer questions based on the provided context.
            Be concise and accurate.""")
    String chat(@UserMessage String message);
}