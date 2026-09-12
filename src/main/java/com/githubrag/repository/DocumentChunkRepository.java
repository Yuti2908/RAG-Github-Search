package com.githubrag.repository;

import com.githubrag.model.entity.DocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, Long> {

    /**
     * Phase 1: brute-force cosine similarity is computed in-memory
     * (VectorSearchService), so we just need to pull candidate chunks.
     * These finder methods support the optional repo/type filters.
     */
    List<DocumentChunk> findByRepoDocument_RepoName(String repoName);

    List<DocumentChunk> findByChunkType(DocumentChunk.ChunkType chunkType);

    List<DocumentChunk> findByRepoDocument_RepoNameAndChunkType(
            String repoName, DocumentChunk.ChunkType chunkType);
}
