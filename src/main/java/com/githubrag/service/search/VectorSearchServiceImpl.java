package com.githubrag.service.search;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.githubrag.model.dto.ChunkResult;
import com.githubrag.model.entity.DocumentChunk;
import com.githubrag.repository.DocumentChunkRepository;
import com.githubrag.service.embedding.EmbeddingService;
import com.githubrag.util.CosineSimilarityUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
public class VectorSearchServiceImpl implements VectorSearchService {

    private final DocumentChunkRepository documentChunkRepository;
    private final EmbeddingService embeddingService;
    private final ObjectMapper objectMapper;

    public VectorSearchServiceImpl(
            DocumentChunkRepository documentChunkRepository,
            EmbeddingService embeddingService,
            ObjectMapper objectMapper) {
        this.documentChunkRepository = documentChunkRepository;
        this.embeddingService = embeddingService;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<ChunkResult> search(String query, int topK, String repoName, String chunkType) {
        List<DocumentChunk> candidates = fetchCandidates(repoName, chunkType);
        if (candidates.isEmpty()) {
            return List.of();
        }

        List<Float> queryEmbedding = embeddingService.embed(query);

        return candidates.stream()
                .map(chunk -> toScoredResult(chunk, queryEmbedding))
                .sorted(Comparator.comparingDouble(ChunkResult::getSimilarityScore).reversed())
                .limit(topK)
                .toList();
    }

    private List<DocumentChunk> fetchCandidates(String repoName, String chunkType) {
        boolean hasRepoFilter = repoName != null && !repoName.isBlank();
        boolean hasTypeFilter = chunkType != null && !chunkType.isBlank();

        if (hasRepoFilter && hasTypeFilter) {
            DocumentChunk.ChunkType type = DocumentChunk.ChunkType.valueOf(chunkType.toUpperCase());
            return documentChunkRepository.findByRepoDocument_RepoNameAndChunkType(repoName, type);
        }
        if (hasRepoFilter) {
            return documentChunkRepository.findByRepoDocument_RepoName(repoName);
        }
        if (hasTypeFilter) {
            DocumentChunk.ChunkType type = DocumentChunk.ChunkType.valueOf(chunkType.toUpperCase());
            return documentChunkRepository.findByChunkType(type);
        }
        return documentChunkRepository.findAll();
    }

    private ChunkResult toScoredResult(DocumentChunk chunk, List<Float> queryEmbedding) {
        List<Float> chunkEmbedding = deserializeEmbedding(chunk.getEmbedding());
        double score = CosineSimilarityUtil.cosineSimilarity(queryEmbedding, chunkEmbedding);

        return ChunkResult.builder()
                .repoName(chunk.getRepoDocument().getRepoName())
                .filePath(chunk.getRepoDocument().getFilePath())
                .content(chunk.getContent())
                .chunkIndex(chunk.getChunkIndex())
                .similarityScore(score)
                .build();
    }

    private List<Float> deserializeEmbedding(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<Float>>() {
            });
        } catch (Exception e) {
            log.warn("Failed to deserialize embedding, skipping chunk: {}", e.getMessage());
            return List.of();
        }
    }
}
