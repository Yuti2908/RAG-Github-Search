package com.githubrag.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request body for POST /api/v1/repos/ingest.
 * <p>
 * Phase 1: {@code githubUsername} defaults to the configured owner and
 * {@code repoName} is optional (ingest all public repos if absent).
 * Phase 2: allows ingesting an arbitrary public repo on demand.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IngestRequest {

    /** GitHub username/owner to ingest from. Falls back to app config if null. */
    private String githubUsername;

    /** Specific repo to ingest. If null, all public repos for the user are ingested. */
    private String repoName;

    /** Force re-ingestion even if the repo hasn't changed since last run. */
    @Builder.Default
    private boolean forceRefresh = false;
}
