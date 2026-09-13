package com.githubrag.service.search;

import com.githubrag.model.dto.ChunkResult;

import java.util.List;

public interface VectorSearchService {

    /**
     * Embeds the query, ranks matching chunks by cosine similarity, and
     * returns the top-K results. repoName and chunkType are optional filters
     * applied before ranking.
     */
    List<ChunkResult> search(String query, int topK, String repoName, String chunkType);
}
