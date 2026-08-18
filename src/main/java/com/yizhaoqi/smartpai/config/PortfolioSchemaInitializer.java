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
        jdbcTemplate.execute("""
                create table if not exists users (
                    id bigserial primary key,
                    username varchar(255) not null unique,
                    password varchar(255) not null,
                    role varchar(32) not null,
                    org_tags varchar(255),
                    primary_org varchar(255),
                    created_at timestamp,
                    updated_at timestamp
                )
                """);
        jdbcTemplate.execute("""
                create table if not exists portfolio_document_chunks (
                    id bigserial primary key,
                    document_id varchar(128) not null,
                    title varchar(255) not null,
                    chunk_index integer not null,
                    content text not null,
                    page_number integer,
                    section varchar(255),
                    is_public boolean not null default true,
                    created_at timestamp not null default current_timestamp,
                    embedding vector(1024)
                )
                """);
        jdbcTemplate.execute("create index if not exists idx_portfolio_chunks_public on portfolio_document_chunks (is_public)");
        jdbcTemplate.execute("create index if not exists idx_portfolio_chunks_document on portfolio_document_chunks (document_id)");
        jdbcTemplate.execute("create index if not exists idx_portfolio_chunks_embedding on portfolio_document_chunks using hnsw (embedding vector_cosine_ops)");
    }
}
