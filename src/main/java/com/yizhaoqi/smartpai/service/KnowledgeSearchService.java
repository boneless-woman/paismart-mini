package com.yizhaoqi.smartpai.service;

import com.yizhaoqi.smartpai.entity.SearchResult;

import java.util.List;

/** Search boundary shared by the full Elasticsearch runtime and the lightweight portfolio runtime. */
public interface KnowledgeSearchService {
    List<SearchResult> searchWithPermission(String query, String userId, int topK);

    List<SearchResult> search(String query, int topK);
}
