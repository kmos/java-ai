package io.mosfet.rag.cli;

import java.util.Scanner;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.milvus.MilvusEmbeddingStore;
import io.quarkiverse.langchain4j.ModelName;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;
import picocli.CommandLine.Command;

@Command(name = "RagCommand", mixinStandardHelpOptions = true)
public class RagCommand implements Runnable {

    @Inject
    Chat chat;

    @Inject
    MilvusStore embeddingStore;

    @Inject
    DocumentDatabase documentDatabase;

    @Inject
    MilvusEmbeddingStore milvusEmbeddingStore;

    @Inject
    @ModelName("granite")
    EmbeddingModel embeddingModel;

    @Override
    public void run() {
        System.out.println("RAG CLI - type a question to chat, or use a /command");
        System.out.println("Commands: /init, /list-milvus, /list-document, /insert-document <paper-id>, /delete-document <paper-id>, /search <query>, /exit");
        System.out.println();

        try (var scanner = new Scanner(System.in)) {
            while (true) {
                System.out.print("> ");
                if (!scanner.hasNextLine()) {
                    break;
                }
                var line = scanner.nextLine().trim();
                if (line.isEmpty()) {
                    continue;
                }

                try {
                    if (line.startsWith("/")) {
                        if (!handleCommand(line)) {
                            break;
                        }
                    } else {
                        handleQuery(line);
                    }
                } catch (Exception e) {
                    Log.errorf("Error: %s", e.getMessage());
                }
            }
        }
    }

    private boolean handleCommand(String line) throws Exception {
        var parts = line.split("\\s+", 2);
        var command = parts[0].toLowerCase();
        var arg = parts.length > 1 ? parts[1].trim() : null;

        switch (command) {
            case "/exit":
                return false;
            case "/init":
                embeddingStore.init();
                documentDatabase.init();
                break;
            case "/list-milvus":
                embeddingStore.list();
                break;
            case "/list-document":
                documentDatabase.listPapers();
                break;
            case "/insert-document":
                if (arg == null || arg.isEmpty()) {
                    System.err.println("Usage: /insert-document <arXiv-id>");
                    break;
                }
                documentDatabase.insert(arg);
                break;
            case "/delete-document":
                if (arg == null || arg.isEmpty()) {
                    System.err.println("Usage: /delete-document <arXiv-id>");
                    break;
                }
                documentDatabase.delete(arg);
                break;
            case "/search":
                if (arg == null || arg.isEmpty()) {
                    System.err.println("Usage: /search <query>");
                    break;
                }
                handleSearch(arg);
                break;
            default:
                System.err.println("Unknown command: " + command);
                System.err.println("Commands: /init, /list-milvus, /list-document, /insert-document <id>, /delete-document <id>, /search <query>, /exit");
                break;
        }
        return true;
    }

    @ActivateRequestContext
    void handleQuery(String query) {
        Log.infof("Sending query: %s", query);
        var reply = chat.chat(query);
        System.out.println(reply);
    }

    void handleSearch(String query) {
        System.out.println("Embedding query...");
        var embedding = embeddingModel.embed(query).content();
        System.out.printf("Embedding dimension: %d%n", embedding.dimension());

        System.out.println("Searching Milvus...");
        var searchRequest = EmbeddingSearchRequest.builder()
                .queryEmbedding(embedding)
                .maxResults(3)
                .minScore(0.0)
                .build();
        var results = milvusEmbeddingStore.search(searchRequest);

        System.out.printf("Found %d results:%n", results.matches().size());
        for (var match : results.matches()) {
            System.out.printf("  score=%.4f id=%s text=%s%n",
                    match.score(),
                    match.embeddingId(),
                    match.embedded() != null ? match.embedded().text().substring(0, Math.min(100, match.embedded().text().length())) + "..." : "NULL");
        }
    }
}