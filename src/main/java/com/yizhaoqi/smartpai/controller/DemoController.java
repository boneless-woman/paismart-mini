package com.yizhaoqi.smartpai.controller;

import com.yizhaoqi.smartpai.config.PortfolioDemoProperties;
import com.yizhaoqi.smartpai.repository.PortfolioDocumentChunkRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@Profile("portfolio-demo")
@RequestMapping("/api/v1/demo")
public class DemoController {
    private final JdbcTemplate jdbcTemplate;
    private final PortfolioDocumentChunkRepository chunks;
    private final PortfolioDemoProperties properties;

    public DemoController(JdbcTemplate jdbcTemplate, PortfolioDocumentChunkRepository chunks, PortfolioDemoProperties properties) {
        this.jdbcTemplate = jdbcTemplate;
        this.chunks = chunks;
        this.properties = properties;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        boolean databaseReady;
        try {
            databaseReady = Boolean.TRUE.equals(jdbcTemplate.queryForObject("select true", Boolean.class));
        } catch (Exception ignored) {
            databaseReady = false;
        }
        long publicChunks = databaseReady ? chunks.countByPublicChunkTrue() : 0;
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("status", databaseReady && publicChunks > 0 ? "ready" : "warming");
        data.put("database", databaseReady ? "ready" : "unavailable");
        data.put("knowledgeBase", publicChunks > 0 ? "ready" : "empty");
        data.put("publicChunks", publicChunks);
        data.put("readOnly", true);
        data.put("demoUsername", properties.getUsername());
        return Map.of("code", 200, "message", "Portfolio demo health", "data", data);
    }
}
