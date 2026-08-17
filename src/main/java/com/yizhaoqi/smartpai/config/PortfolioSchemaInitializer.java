package com.yizhaoqi.smartpai.config;

import org.springframework.context.annotation.Profile;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Profile("portfolio-demo")
@Order(10)
public class PortfolioSchemaInitializer implements ApplicationRunner {
    private final JdbcTemplate jdbcTemplate;

    public PortfolioSchemaInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        jdbcTemplate.execute("create extension if not exists vector");
        jdbcTemplate.execute("alter table portfolio_document_chunks add column if not exists embedding vector(1024)");
        jdbcTemplate.execute("create index if not exists idx_portfolio_chunks_embedding on portfolio_document_chunks using hnsw (embedding vector_cosine_ops)");
    }
}
