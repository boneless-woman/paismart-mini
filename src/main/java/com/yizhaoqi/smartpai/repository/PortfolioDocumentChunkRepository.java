package com.yizhaoqi.smartpai.repository;

import com.yizhaoqi.smartpai.model.PortfolioDocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PortfolioDocumentChunkRepository extends JpaRepository<PortfolioDocumentChunk, Long> {
    long countByPublicChunkTrue();
}
