package com.githubrag.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a single file pulled from a GitHub repository (e.g. README.md,
 * a source file). Holds the raw, unmodified content as fetched from GitHub.
 * Chunking + embedding happen downstream and are stored in {@link DocumentChunk}.
 */
@Entity
@Table(name = "repo_document", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"repo_name", "file_path"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RepoDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "repo_name", nullable = false)
    private String repoName;

    @Column(name = "repo_owner", nullable = false)
    private String repoOwner;

    @Column(name = "file_path", nullable = false)
    private String filePath;

    /**
     * High-level type used to pick a chunking strategy downstream.
     * e.g. README, JAVA, PYTHON, MARKDOWN, OTHER
     */
    @Column(name = "file_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private FileType fileType;

    @Lob
    @Column(name = "raw_content", nullable = false)
    private String rawContent;

    @Column(name = "last_ingested_at", nullable = false)
    private Instant lastIngestedAt;

    @OneToMany(mappedBy = "repoDocument", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DocumentChunk> chunks = new ArrayList<>();

    public enum FileType {
        README,
        MARKDOWN,
        JAVA,
        PYTHON,
        JAVASCRIPT,
        TYPESCRIPT,
        YAML,
        OTHER
    }
}
