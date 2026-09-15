package io.mosfet.rag.cli;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

import javax.sql.DataSource;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class DocumentDatabase {

    @ConfigProperty(name = "debezium.rag.demo.document.truncate", defaultValue = "2048")
    int documentSize;

    @Inject
    DataSource dataSource;

    @Inject
    ObjectMapper objectMapper;

    public void init() throws Exception {
        try (final var conn = dataSource.getConnection()) {
            conn.createStatement().execute("TRUNCATE TABLE ai.documents");
        }
    }

    public void delete(String paperId) throws Exception {
        final var sql = "DELETE FROM ai.documents WHERE id = ?";

        try (final var conn = dataSource.getConnection();
                final var stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, paperId);
            stmt.executeUpdate();
        }
    }

    public void insert(String paperId) throws Exception {
        final var paper = loadPaper(paperId);

        final var text = "# Title\n%s\n\n# Authors\n%s\n\n# Abstract\n%s"
                .formatted(paper.title, paper.authors, paper.abstractText);
        final var truncated = text.substring(0, Math.min(text.length(), documentSize));

        final var sql = "INSERT INTO ai.documents VALUES (?, ?::json, ?)";
        try (final var conn = dataSource.getConnection();
                final var stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, paperId);
            stmt.setString(2, objectMapper.writeValueAsString(new PaperMetadata(paperId, paper.title)));
            stmt.setString(3, truncated);
            stmt.executeUpdate();
        }
        Log.infof("Inserted paper '%s': %s", paperId, paper.title);
    }

    public void listPapers() throws IOException, URISyntaxException {
        var classLoader = Thread.currentThread().getContextClassLoader();
        var resource = classLoader.getResource("papers/");
        if (resource == null) {
            System.out.println("No papers directory found.");
            return;
        }

        Path papersDir;
        if ("jar".equals(resource.toURI().getScheme())) {
            var fs = FileSystems.newFileSystem(resource.toURI(), Collections.emptyMap());
            papersDir = fs.getPath("papers/");
        } else {
            papersDir = Paths.get(resource.toURI());
        }

        System.out.println("Available papers:");
        try (var stream = Files.list(papersDir)) {
            stream
                .filter(p -> p.getFileName().toString().endsWith(".json"))
                .sorted()
                .forEach(p -> {
                    var fileName = p.getFileName().toString();
                    var id = fileName.substring(0, fileName.length() - ".json".length());
                    try {
                        var paper = loadPaper(id);
                        System.out.printf("  %-20s %s%n", id, paper.title());
                    } catch (IOException e) {
                        System.out.printf("  %-20s (error reading: %s)%n", id, e.getMessage());
                    }
                });
        }
    }

    private Paper loadPaper(String paperId) throws IOException {
        final var path = "papers/" + paperId + ".json";
        try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(path)) {
            if (is == null) {
                throw new IOException("Paper not found in resources: " + path);
            }
            return objectMapper.readValue(is, Paper.class);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Paper(String title, String authors, @com.fasterxml.jackson.annotation.JsonProperty("abstract") String abstractText) {}

    record PaperMetadata(String id, String title) {}
}
