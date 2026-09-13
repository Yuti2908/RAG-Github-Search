package com.githubrag.model.dto;

import com.githubrag.model.entity.DocumentChunk;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents a chunk of text before it has been embedded/persisted.
 * ChunkingService produces these; EmbeddingService + DocumentIngestionService
 * turn them into DocumentChunk entities.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TextChunk {

    private int chunkIndex;
    private String content;
    private DocumentChunk.ChunkType chunkType;
}
