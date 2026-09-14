package io.mosfet.rag.cli;

import java.util.function.Supplier;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.rag.AugmentationRequest;
import dev.langchain4j.rag.AugmentationResult;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.injector.DefaultContentInjector;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.milvus.MilvusEmbeddingStore;
import io.quarkiverse.langchain4j.ModelName;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class MilvusRetrievalAugmentor implements Supplier<RetrievalAugmentor> {

    private final RetrievalAugmentor augmentor;

    MilvusRetrievalAugmentor(MilvusEmbeddingStore store, @ModelName("granite") EmbeddingModel model) {
        EmbeddingStoreContentRetriever contentRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingModel(model)
                .embeddingStore(store)
                .maxResults(3)
                .build();
        var contentInjector = DefaultContentInjector.builder()
                .promptTemplate(PromptTemplate.from("""
                        Context:
                        {{contents}}

                        Question: {{userMessage}}"""))
                .build();
        augmentor = new RetrievalAugmentorDecorator(DefaultRetrievalAugmentor
                .builder()
                .contentRetriever(contentRetriever)
                .contentInjector(contentInjector)
                .build());
    }

    @Override
    public RetrievalAugmentor get() {
        return augmentor;
    }

    private class RetrievalAugmentorDecorator implements RetrievalAugmentor {

        private final RetrievalAugmentor delegate;

        public RetrievalAugmentorDecorator(RetrievalAugmentor delegate) {
            this.delegate = delegate;
        }

        @Override
        public AugmentationResult augment(AugmentationRequest augmentationRequest) {
            Log.debugf("Requested augmentation of %s", augmentationRequest.chatMessage());
            final var result = delegate.augment(augmentationRequest);
            Log.debugf("Augmentation retrieved %d contents", result.contents().size());
            Log.debugf("Augmented message: %s", result.chatMessage());
            return result;
        }
    }
}