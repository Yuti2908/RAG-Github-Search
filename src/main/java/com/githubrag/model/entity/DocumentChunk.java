package com.githubrag.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A chunk of text derived from a {@link RepoDocument}, along with its
 * embedding vector. The embedding is stored as a JSON-serialized array of
 * doubles for now (matches the reference RAG project's approach with H2);
 * swap for a native `vector` column when moving to pgvector.
 */
@Entity
@Table(name = "document_chunk")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "repo_document_id", nullable = false)
    private RepoDocument repoDocument;

    @Column(name = "chunk_index", nullable = false)
    private Integer chunkIndex;

    /**
     * Whether this chunk came from prose (README/markdown) or source code.
     * Chunking strategy and future re-ranking logic can branch on this.
     */
    @Column(name = "chunk_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private ChunkType chunkType;

    @Lob
    @Column(name = "content", nullable = false)
    private String content;

    /**
     * JSON-serialized float array, e.g. "[0.0123, -0.0456, ...]".
     * Kept as text for H2 simplicity; parsed at query time for cosine similarity.
     */
    @Lob
    @Column(name = "embedding", nullable = false)
    private String embedding;

    public enum ChunkType {
        PROSE,
        CODE
    }
}
