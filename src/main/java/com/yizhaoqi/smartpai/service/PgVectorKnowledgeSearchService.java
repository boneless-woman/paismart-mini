package com.yizhaoqi.smartpai.service;

import com.yizhaoqi.smartpai.client.EmbeddingClient;
import com.yizhaoqi.smartpai.entity.SearchResult;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@Profile("portfolio-demo")
public class PgVectorKnowledgeSearchService implements KnowledgeSearchService {
    private final JdbcTemplate jdbcTemplate;
    private final EmbeddingClient embeddingClient;

    public PgVectorKnowledgeSearchService(JdbcTemplate jdbcTemplate, EmbeddingClient embeddingClient) {
        this.jdbcTemplate = jdbcTemplate;
        this.embeddingClient = embeddingClient;
    }

    @Override
    public List<SearchResult> searchWithPermission(String query, String userId, int topK) {
        return search(query, topK);
    }

    @Override
    public List<SearchResult> search(String query, int topK) {
        if (query == null || query.isBlank()) return List.of();
        float[] vector = embeddingClient.embed(List.of(query), "portfolio-demo", EmbeddingClient.UsageType.QUERY).get(0);
        String literal = toVectorLiteral(vector);
        int limit = Math.max(1, Math.min(topK, 10));
        return jdbcTemplate.query("""
                select document_id, chunk_index, content, title, page_number, section,
                       greatest(0, 1 - (embedding <=> cast(? as vector))) as score
                  from portfolio_document_chunks
                 where is_public = true and embedding is not null
                 order by embedding <=> cast(? as vector)
                 limit ?
                """, (rs, rowNum) -> new SearchResult(
                        rs.getString("document_id"),
                        rs.getInt("chunk_index"),
                        rs.getString("content"),
                        rs.getDouble("score"),
                        "portfolio-demo",
                        "PUBLIC",
                        true,
                        rs.getString("title"),
                        (Integer) rs.getObject("page_number"),
                        rs.getString("section"),
                        "PGVECTOR",
                        rs.getString("content")
                ), literal, literal, limit);
    }

    private String toVectorLiteral(float[] vector) {
        return IntStream.range(0, vector.length)
                .mapToObj(i -> String.format(Locale.ROOT, "%.8f", vector[i]))
                .collect(Collectors.joining(",", "[", "]"));
    }
}
