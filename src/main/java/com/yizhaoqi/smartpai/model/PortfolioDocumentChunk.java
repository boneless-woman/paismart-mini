package com.yizhaoqi.smartpai.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "portfolio_document_chunks", indexes = {
        @Index(name = "idx_portfolio_chunks_public", columnList = "is_public"),
        @Index(name = "idx_portfolio_chunks_document", columnList = "document_id")
})
public class PortfolioDocumentChunk {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "document_id", nullable = false, length = 128)
    private String documentId;

    @Column(nullable = false)
    private String title;

    @Column(name = "chunk_index", nullable = false)
    private Integer chunkIndex;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    private Integer pageNumber;
    private String section;

    @Column(name = "is_public", nullable = false)
    private boolean publicChunk = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
