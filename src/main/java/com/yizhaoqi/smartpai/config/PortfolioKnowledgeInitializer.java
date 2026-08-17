package com.yizhaoqi.smartpai.config;

import com.yizhaoqi.smartpai.client.EmbeddingClient;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Component
@Profile("portfolio-demo")
@Order(15)
public class PortfolioKnowledgeInitializer implements ApplicationRunner {
    private final JdbcTemplate jdbcTemplate;
    private final EmbeddingClient embeddingClient;

    @Value("${portfolio.demo.seed-path:docs/paismart.md}")
    private String seedPath;

    @Value("${portfolio.demo.seed-title:PaiSmart 项目说明}")
    private String seedTitle;

    public PortfolioKnowledgeInitializer(JdbcTemplate jdbcTemplate, EmbeddingClient embeddingClient) {
        this.jdbcTemplate = jdbcTemplate;
        this.embeddingClient = embeddingClient;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        Path source = Path.of(seedPath);
        if (!Files.isRegularFile(source)) return;
        String content = Files.readString(source, StandardCharsets.UTF_8);
        String documentId = sha256(content);
        Integer existing = jdbcTemplate.queryForObject(
                "select count(*) from portfolio_document_chunks where document_id = ?", Integer.class, documentId);
        if (existing != null && existing > 0) return;

        List<String> chunks = chunk(content, 900);
        List<float[]> vectors = embeddingClient.embed(chunks, "system-portfolio-seed", EmbeddingClient.UsageType.UPLOAD);
        for (int i = 0; i < chunks.size(); i++) {
            jdbcTemplate.update("""
                    insert into portfolio_document_chunks
                    (document_id, title, chunk_index, content, is_public, created_at, embedding)
                    values (?, ?, ?, ?, true, current_timestamp, cast(? as vector))
                    """, documentId, seedTitle, i, chunks.get(i), toVectorLiteral(vectors.get(i)));
        }
    }

    private List<String> chunk(String content, int maxLength) {
        String normalized = content.replace("\r\n", "\n").trim();
        List<String> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String paragraph : normalized.split("\n{2,}")) {
            if (current.length() > 0 && current.length() + paragraph.length() + 2 > maxLength) {
                chunks.add(current.toString());
                current.setLength(0);
            }
            if (paragraph.length() > maxLength) {
                if (current.length() > 0) {
                    chunks.add(current.toString());
                    current.setLength(0);
                }
                for (int start = 0; start < paragraph.length(); start += maxLength) {
                    chunks.add(paragraph.substring(start, Math.min(start + maxLength, paragraph.length())));
                }
            } else {
                if (current.length() > 0) current.append("\n\n");
                current.append(paragraph);
            }
        }
        if (current.length() > 0) chunks.add(current.toString());
        return chunks;
    }

    private String toVectorLiteral(float[] vector) {
        return IntStream.range(0, vector.length)
                .mapToObj(i -> String.format(Locale.ROOT, "%.8f", vector[i]))
                .collect(Collectors.joining(",", "[", "]"));
    }

    private String sha256(String value) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    }
}
